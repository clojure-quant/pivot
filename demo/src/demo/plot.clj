(ns study.pivot
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]
   [quanta.calendar.window :as w]
   [pivot.calc :refer [calc-multi-window calc-multi-window-multi-assets]]
   [pivot.db]
   [demo.env :refer [env]]))

(def calendar [:forex :m])

(def pivots
  (calc-multi-window
   env
   {:asset "EURUSD"
    :calendar calendar
    :windows [240 480 720 1440
            ;4h 8h  12h 24h
              ]
    :window (w/create-trailing-window calendar 50000 (t/instant "2025-04-21T22:00:00Z"))}))

pivots

(def spec-single-ds
  {;:$schema "https://vega.github.io/schema/vega-lite/v4.json"
   :width 800
   :height 800
   :layer [{:mark {:type "point" :color "blue"}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-price" :type "quantitative"
                           :scale {:zero false}}}}]})

(->> (plot/vegalite-ds env {:cols [:pivot-price :idx :n]
                            :spec spec-single-ds} pivots)
     tap>)


(def spec-multiple-ds
  {:width 800
   :height 800
   :layer [{:mark {:type "point" :filled true}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-price" :type "quantitative"
                           :scale {:zero false}}
                       ;:color {:field "n" :type "nominal"}
                       ;:color {:field "n" :type "ordinal"
                       ;        :scale {:range ["#d4f0ff" "#74c2e1" "#1e90c7" "#1266a0" "#08306b"]}}
                       :color {:field "n" :type "ordinal"
                               :scale {:range ["yellow" "violet" "blue" "black"]}}}}]})


(->> pivots
     (plot/vegalite-ds env {:cols [:pivot-price :idx :n]
                            :spec spec-multiple-ds})
     tap>)



(defn vegalite-multiple-pivots [ds pivots]
  (plot/vegalite-ds
   env
   {:cols [:pivot-price :idx :n :date]
    :spec  {:width 1000
            :height 1000
            :layer (concat
                    [{:mark {:type "point" :filled true}
                      :encoding {:x {:field "idx" :type "quantitative"}
                                 :y {:field "pivot-price" :type "quantitative"
                                     :scale {:zero false}}
                                 :color {:field "n" :type "ordinal"
                                         :scale {:range ["yellow" "violet" "blue" "black"]}}
                                 :tooltip [{:field "date" :type "temporal"}
                                           {:field "pivot-high" :type "quantitative"}
                                           {:field "pivot-low" :type "quantitative"}]}}]
                    (map (fn [y-val]
                           {:mark {:type "rule" :color "gray"}
                            :encoding {:y {:datum y-val
                                           :type "quantitative"}
                                       :size {:value 1}}})
                         pivots))}}
   ds))


(-> pivots
    (vegalite-multiple-pivots [1.10 1.12])
    tap>)

(defn lines [middle step nr]
  (->> (concat
        (map  #(+ middle (* step %)) (range nr))
        (map  #(- middle (* step %)) (range nr)))
       (into [])))

(lines 189.0 1.5 7)


(-> pivots
    (vegalite-multiple-pivots
     (lines 1.1 0.01 5))
    tap>)







