(ns timeline.d3
  (:require  [cljsjs.d3]
             [reagent.core :as reagent]
             [cljs-bean.core :refer [bean ->clj ->js]]))


;; Implementing all the d3 functions

(def d3 js/d3)


(defn jsonify-this
  [this]
  (-> this reagent/dom-node ->js))


(defn compute-x-bounds
  [data time-key]
  (let [dates (->> data (map #(get % time-key)) ->js)]
    [(d3.min dates) (d3.max dates)]))


(defn translate-str
  "Helper to generate translate strings."
  [x y]
  (str "translate(" x "," y ")"))


(defn generate-x-scale
  "Defines an x-scale over a period of time."
  [width [start-time end-time]]
  (-> d3 .scaleTime
      (.domain (->js [start-time end-time])) ;; has to be insts
      (.range (->js [0 width]))))


(defn generate-y-scale
  "Defines y-scale along fixed intervals. This gives us rows of data rather than a typical chart."
  [height group-names]
  (let [line-height (/ height (count group-names))] ;; Uniform row heights over all rows
    (-> d3 .scaleOrdinal
        (.domain (->js group-names))
        (.range (->js (map-indexed (fn [i d] (* i line-height)) group-names))))))


(defn x-axis
  "Generates x-axis (bottom justified). Date format and tick amounts passed in from props."
  [width x-scale config]
  (-> d3 .axisBottom (.scale x-scale) (.ticks (:num-ticks config)) (.tickFormat (:tick-format config))))


(defn axes
  "Generates axes."
  [axes-group x-scale height width config]
  (fn [data]
    (let [axis (fn [scale]
                 (let [selection (-> axes-group
                                   (.selectAll (str ".x-axis.focus"))
                                   (.data #js [{}]))] ;; We don't need an x-axis for every row
                   (-> selection .enter (.append "g")
                       (.classed "x-axis" true)
                       (.classed "focus" true)
                       (.attr "fill" "black")
                       (.attr "stroke" "black")
                       (.call (x-axis width scale config))
                       (.attr "transform" (translate-str 0 height)))
                   (-> selection (.call (x-axis width scale config)))
                   (-> selection .exit .remove)))]
      (axis x-scale))))


(defn labels
  "Generates y-axis labels."
  [labels-group height line-height y-scale {:keys [label-key data-key]} config]
  (fn [data]
    (let [lbls (-> labels-group
                 (.selectAll ".label")
                 (.data data))
          text (fn [data] ;; Generates the label text
                 (let [data (->clj data)
                       count-n (count (get data data-key))]
                   (str (get data label-key) (when (>= count-n 0) (str " (" count-n ")")))))]

      ;; Set text
      (-> lbls (.text text))
      (-> lbls .enter (.append "text")
          (.classed "label" true)
          (.attr "transform" (fn [d idx] (translate-str (- (-> config :label-width) 20) (+ (y-scale idx) (/ line-height 2)))))
          (.attr "dominant-baseline" "central") (.attr "text-anchor" "end")
          (.text text))

      (-> lbls .exit .remove))))


(defn draw-base
  "Generates base and handlers."
  [elt chart-group base-group stamp-group x-scale height width line-height config]
  (let [grid-rect (-> base-group (.append "rect") ;; Group spanning entire chart
                      (.attr "width" width) (.attr "height" height)
                      (.classed "grid-rect" true))
        domain (.domain x-scale)
        ts-format (:timestamp-format config)
        dt (str (ts-format (second domain)))
        time-stamp (-> stamp-group (.append "text") (.text (->js dt)) ;; Timestamp over hover marker
                       (.attr "transform" (translate-str (-> x-scale .range ->clj second) 0))
                       (.attr "fill" "black")
                       (.attr "text-anchor" "middle") (.attr "height" height))
        popover (-> elt (.select "#popover") (.style "display" "none") (.attr "text-anchor" "middle"))
        time-box (-> stamp-group
                   (.append "rect")
                   (.attr "height" "24") (.attr "width" "150")
                   (.attr "fill" "none")
                   (.attr "text-anchor" "middle")
                   (.style "display" "none"))
        marker (-> base-group
                 (.append "line")
                 (.attr "fill" "black") (.attr "stroke" "black")
                 (.attr "text-anchor" "middle")
                 (.classed "marker" true) ;; Hover marker
                 (.attr "y1" 0) (.attr "y2" height))]
    (-> base-group
      (.attr "fill" "url(#grid-stripes)")
      (.on "mouseover" (fn []  (-> marker (.style "display" nil))
                         (-> time-stamp (.style "display" nil))
                         (-> time-box (.style "display" nil))))
      (.on "mouseout"  (fn [] (-> marker (.style "display" "none"))
                         (-> time-stamp (.style "display" "none"))
                         (-> time-box (.style "display" "none"))))
      (.on "mousemove" (fn [d i e]
                         (let [x (-> d3 .-event .-layerX)
                               offset-x (- x (-> config :label-width) (-> config :padding :left))
                               y (-> d3 .-event .-layerY)]
                           (if (= x 0) ;; catching weird erroneous mousemove events
                             (-> marker (.style "display" "none"))
                             (do (-> marker (.attr "transform" (translate-str offset-x 0)))
                                 (-> time-box (.attr "transform" (translate-str offset-x -25)))
                                 (-> popover (.style "top" (str y "px")) (.style "left" (str x "px")))
                                 (-> time-stamp (.attr "transform" (translate-str offset-x -9))
                                     (.text (-> x-scale (.invert offset-x) ts-format)))))))))))


(defn events
  "Generates events."
  [elt chart-svg events-group x-scale y-scale line-height
   {:keys [event-hover event-click]}
   {:keys [data-key date-key color-map color-key]}
   config]
  (fn [data]
    (let [popover (-> elt (.select "#popover") (.attr "text-anchor" "middle"))
          event-lines (-> events-group (.selectAll ".event-line") (.data data))
          ;; Creates a group per row and translates it to the correct offset
          event-lines (-> event-lines .enter (.append "g") (.classed "event-line" true)
                          (.attr "transform" (fn [d idx] (translate-str 0 (+ (y-scale idx) (-> line-height (/ 2))))))
                          (.attr "fill" "none"))]
      (.each event-lines (fn [event]
                           (this-as this ;; Gets row element
                             (let [d (-> event ->clj (get data-key) ->js)
                                   events (-> d3 (.select (jsonify-this this)) (.selectAll ".event")
                                              (.data d))
                                   shape (-> events .enter ;;
                                             (.append "text")
                                             (.classed "event" true)
                                             (.attr "transform" (fn [d i]
                                                                  (translate-str (x-scale (-> d ->clj (get date-key) ->js)) 0)))
                                             (.attr "fill" (fn [d] ;; color individual drops
                                                             (let [k (-> d ->clj (get color-key))]
                                                               (get color-map k))))
                                             (.attr "text-anchor" "middle")
                                             (.attr "data-toggle" "popover" ) (.attr "data-html" "true")
                                             (.text (-> config :text-shape)))]
                               (-> shape ;; Popover fn used here
                                 (.on "mouseover" (fn [data]
                                                    (-> popover (.style "display" nil))
                                                    (event-hover data)))
                                 (.on "mouseout" (fn [] (-> popover (.style "display" "none")))))
                               (-> events .exit (.on "click" nil) (.on "mouseover" nil) .remove)))))
      (-> event-lines .exit .remove))))

(defn paths-and-patterns
  "Generates clip paths and patterns. Clip paths define svg regions that are drawable, and interactive.
  Patterns describe things like grid stripes and polygons."
  [svg width height line-height]
  (let [defs (-> svg (.append "defs"))
        pattern (->  defs (.append "pattern"))]

    ;; Defines events clip path
    (-> defs (.append "clipPath")
        (.attr "id" "events-clipper")
        (.append "rect")
        (.attr "id" "events-rect")
        (.attr "x" 0) (.attr "y" 0)
        (.attr "width" width) (.attr "height" height))

    ;; Alternating gray/white rows
    (-> pattern
      (.attr "id" "grid-stripes")
      (.attr "width" width) (.attr "height" (* line-height 2))
      (.attr "patternUnits" "userSpaceOnUse")
      (.attr "fill" "whitesmoke"))
    (-> pattern (.append "rect")
        (.attr "width" width) (.attr "height" line-height))
    (-> pattern (.append "line")
        (.attr "x1" 0) (.attr "x2" width)
        (.attr "y1" line-height) (.attr "y2" line-height))
    (-> pattern (.append "line")
        (.attr "x1" 0) (.attr "x2" width)
        (.attr "y1" "1px") (.attr "y2" "1px"))
    [defs pattern]))

(defn drawer
  "Pieces together all component functions to form the chart. Returns a function that takes data to draw all the components."
  [elt chart-svg {:keys [width height]} scale lbls handlers accessor-keys config]
  (let [height        (- height (-> config :padding :top) (-> config :padding :bottom))
        line-height   (/ height (count lbls))
        _             (paths-and-patterns chart-svg width height line-height)
        base-group    (-> chart-svg (.append "g") (.classed "base" true)
                          (.attr "transform" (translate-str (+ (-> config :padding :left) (-> config :label-width)) 0)))
        labels-group  (-> chart-svg (.append "g") (.classed "labels" true)
                          (.attr "transform" (translate-str (-> config :padding :left) 0)))
        axes-group    (-> chart-svg (.append "g") (.classed "axes" true)
                          (.attr "transform" (translate-str (+ (-> config :padding :left) (-> config :label-width)) 5)))
        events-group  (-> chart-svg (.append "g") (.classed "events" true)
                          ;; Chops off the empty spaces so our event handlers work on the base grid
                          (.attr "clip-path" "url(#events-clipper)")
                          (.attr "transform" (translate-str (+ (-> config :padding :left) (-> config :label-width)) 0)))
        stamp-group   (-> chart-svg (.append "g")
                          (.classed "timestamp" true)
                          (.attr "height" 30)
                          (.attr "transform"
                                 (translate-str (+ (-> config :padding :left) (-> config :label-width)) (-> config :padding :top))))
        draw-axes     (axes axes-group (:x scale) height width config)
        draw-labels   (labels labels-group height line-height (:y scale) accessor-keys config)
        draw-events   (events elt  chart-svg events-group (:x scale) (:y scale) line-height handlers accessor-keys config)]

    (draw-base elt chart-svg base-group stamp-group (:x scale) height width line-height config)
    ;; Return fn that takes data and generates the chart
    (fn [data]
      (draw-axes data)
      (draw-events data)
      (draw-labels data))))
