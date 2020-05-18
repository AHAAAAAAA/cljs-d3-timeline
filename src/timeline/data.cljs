(ns timeline.data
  (:require [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :as st]
            [cljs.spec.gen.alpha :as gen]
            [clojure.test.check.generators]))

;; Generatively creating sample data to feed into the chart


(def colors ["#ffe119" "#4363d8" "#f58231" "#911eb4" "#46f0f0" "#f032e6" "#bcf60c" "#fabebe" "#008080"])
(def names #{"Ahmed" "Ali" "Alya" "Colleen" "Joe"})
(def statuses #{"Complete" "Partial" "Not Needed"})
(def tasks #{"Clean Bathroom" "Sweep Garden" "Make bed" "Wash kitchen"})

;; Describing a chore list over the past week
(s/def ::description-person names)
(s/def ::description-status statuses)
(s/def ::description-date (s/inst-in (-> (js/Date.)  (- (* 7 24 60 60 1000)) js/Date.)
                             (js/Date.)))
(s/def ::chore-description (s/keys :req [::description-status ::description-date ::description-person]))
(s/def ::chore-name tasks)
(s/def ::chore-data (s/coll-of ::chore-description))
(s/def ::chores (s/keys :req [::chore-name ::chore-data]))
(s/def ::data (s/coll-of ::chores))

;; Generates n rows of data with the supplied parameters
(def raw-data (gen/generate (s/gen ::data)))

;; Group like chores
(def data (->> raw-data
            (reduce (fn [m chore]
                      (update m (::chore-name chore) conj (::chore-data chore)))
                    {})
               (map (fn [[k v]] {::chore-name k
                                 ::chore-data (flatten v)}))))

;; Ideally, this should be supplied with the generative data.
;; It maps every person to a color.
(def color-map (->> names
                 (map-indexed (fn [i n] [n (get colors i)]))
                 (into {})))
