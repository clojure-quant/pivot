(ns demo.algo.dojipivot
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]
   [quanta.calendar.window :as w]
   [pivot.calc :refer [calc-multi-window calc-multi-window-multi-assets
                       only-highest-n-per-date
                       load-bars]]
   [pivot.db]
   [pivot.plot :refer [plot-pivots-blue
                       plot-pivots-n-colored
                       plot-pivots-n-colored-lines
                       add-dt-str]]
   [demo.plot-window :refer [plot-bar-window
                             plot-bar-dt-window]]
   [demo.indicator.doji :refer [add-doji select-dojis-at-high-low]]
   [demo.util :refer [print-ds]]
   [demo.env :refer [env]]))


(defn calc [asset opts]
  (let [calendar [:forex :m]
        calendar-doji [:forex :m10]
        bars (load-bars env {:asset asset
                             :window (w/create-trailing-window calendar-doji 80000 (t/instant "2025-05-07T20:00:00Z"))})
        dojis (-> bars
                  (select-dojis-at-high-low opts)
                  (tc/select-rows (fn [row]  (> (:volume row) 500)))
                  )
        pivots (calc-multi-window
                env
                {:asset asset
                 :calendar calendar
                 :windows [240 480 720 1440
                    ;4h 8h  12h 24h
                           ]
                 :window (w/create-trailing-window calendar 80000 (t/instant "2025-05-07T20:00:00Z"))})
        n-pivots (only-highest-n-per-date pivots)]
    ;
    {:dojis dojis
     :pivots n-pivots}))


(defn plot-pivots-n-colored-doji
  "plots a dataset of pivot points (with different colors for different n levels)"
  [env {:keys [dojis pivots]}]
  (plot/vegalite
   env
   {:spec {:width 1200
           :height 1200
           :layer [; pivot
                   {:data {:name "pivots"}
                    :mark {:type "point" :filled true}
                    :encoding {;:x {:field "idx" :type "quantitative"}
                               :x {:field "date" :type "temporal"}
                               :y {:field "pivot-price" :type "quantitative"
                                   :scale {:zero false}}
                               :color {:field "n" :type "ordinal"
                                       :scale {:range ["yellow" "violet" "blue" "black"]}}
                               :tooltip [{:field "dt-str" :type "nominal"}
                                         {:field "pivot-price" :type "quantitative"}
                                         {:field "pivot-type" :type "nominal"}
                                         {:field "pivot-range" :type "quantitative"}
                                         {:field "pivot-volume" :type "quantitative"}]}}
                   ; doji
                   {:data {:name "dojis"}
                    :mark {:type "rule"
                           ;:type "point" 

                           ;:shape "cross"
                           ;:strokeWidth 1        ;; <- This sets the line width
                           ;:width 1
                           ;:size 30
                           }
                    :encoding {;:x {:field "idx" :type "quantitative"}
                               :x {:field "date" :type "temporal"}
                               ;:y {:field "close" :type "quantitative" :scale {:zero false}}
                               :y {:field "doji-marker-low" :type "quantitative" :scale {:zero false}}
                               :y2 {:field "doji-marker-high" :type "quantitative" :scale {:zero false}}
                               :color {:value "red"}

                               :tooltip [{:field "dt-str" :type "nominal"}
                                         {:field "open" :type "quantitative"}
                                         {:field "high" :type "quantitative"}
                                         {:field "low" :type "quantitative"}
                                         {:field "close" :type "quantitative"}
                                         {:field "doji-marker-low" :type "quantitative"}
                                         {:field "doji-marker-high" :type "quantitative"}
                                         {:field "doji-marker-type" :type "nominal"}
                                         {:field "volume" :type "quantitative"}]}}]}
    :data {:pivots (plot/convert-data (add-dt-str pivots) [:pivot-price :idx :date :n
                                                           :pivot-type  :pivot-range  :pivot-volume :dt-str])
           :dojis (plot/convert-data (add-dt-str dojis) [:close :idx :date :doji-marker-high :doji-marker-low :doji-marker-type
                                                         :dt-str :volume :open :high :low])}}))


(defn calc-plot [asset opts]
  (->> (calc asset opts)
       (plot-pivots-n-colored-doji env)))


(comment
  (calc-plot "EURUSD"
             {:min-wick-percent 0.7
              :range-sma-n 6
              :min-range-percent 0.7
              :trailing-n 6})
  
  (-> (calc "EURUSD"
            {:min-wick-percent 0.7
             :range-sma-n 6
             :min-range-percent 0.7
             :trailing-n 6})
      :dojis
      (tc/select-rows (fn [row]  (> (:volume row) 500)))
      
      )
   

  


  ;
  )

