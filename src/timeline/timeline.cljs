(ns timeline.timeline
  (:require [reagent.core :as reagent :refer [atom]]
            [timeline.data :as t.data]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [timeline.d3 :as t.d3]))

(def default-config
  { ;; A few hardcoded defaults
    :num-ticks 5
    :tick-format (-> t.d3/d3 (.timeFormat "%-m/%-d/%y"))
    :timestamp-format (-> t.d3/d3 (.timeFormat "%-m/%-d/%y - %-H:%-M:%-S"))
    :label-width 175
    :padding {:top 30 :left 0 :bottom 30 :right 20}
    :text-shape "O"})

(defn event-hover
  "Custom event-hover handler generating html to show on mouseover."
  [data] ;; Custom hover handler
  (let [style "<style> #popover {position: absolute; background-color: cornflowerblue; border-color: black transparent transparent transparent;
  padding: 5px 5px; z-index: 1001; min-width: max-content; border-radius: 5px;  </style>"
        clj-data (->clj data)]
    (-> t.d3/d3 (.select "#popover")
        (.html
          (str style
               "Date: " ((.timeFormat t.d3/d3 "%x %I:%M %p") (->js (::t.data/description-date clj-data))) "<br/>"
               (str "Person: " (::t.data/description-person clj-data) "<br/>"
                    "Status: " (::t.data/description-status clj-data) "<br/>"))))))


(defn generate-chart
  "Redraws chart, returns nil.
  Implements all the data-specific logic so the d3 fns can be agnostic to what the data looks like."
  [elt this height width]
  (fn [data]
    (-> t.d3/d3 (.select this) (.select ".timeline-chart") .remove)
    (let [time-bounds (t.d3/compute-x-bounds (flatten (map ::t.data/chore-data data)) ::t.data/description-date)
          labels (set (map ::t.data/chore-name data))
          x-scale (t.d3/generate-x-scale width time-bounds)
          y-scale (t.d3/generate-y-scale height labels)
          handlers {:event-hover event-hover
                    :event-click nil}
          accessor-keys {:data-key ::t.data/chore-data
                         :color-map t.data/color-map
                         :label-key ::t.data/chore-name
                         :color-key ::t.data/description-person
                         :date-key ::t.data/description-date}
          chart-elt (-> t.d3/d3 (.select this) (.append "svg") (.classed "timeline-chart" true)
                      (.style "margin" "auto auto")
                      (.style "display" "block")
                      (.attr "width" width) (.attr "height" height))
          draw-fn (t.d3/drawer elt chart-elt
                               {:width width
                                :height height}
                               {:x x-scale
                                :y y-scale}
                               labels
                               handlers
                               accessor-keys
                               default-config)]
      (draw-fn (->js data)))))

(defn timeline
  [data]
  (let [width 1000
        height 400
        update-graph-fn (fn [this data] ;;
                          (let [this (t.d3/jsonify-this this) ;; DOM element to manipulate
                                elt (-> t.d3/d3 (.select this) ;; Select DOM element
                                        (.classed "timeline-viz" true))]
                            ;; Process data into SVG
                            ((generate-chart elt this height width) data)))]
    (reagent/create-class
      {:component-will-update
       (fn [this [_ data]]
         (update-graph-fn this data))
       :component-did-mount
       (fn [this]
         (update-graph-fn this data))
       :reagent-render
       (fn [data]
         [:div.timeline-viz {:style {:width width :height height :display :block :margin :auto}}
          ;; We create the popover here to avoid some scoping issues.
          [:span#popover {:style {:display :none}}]])})))
