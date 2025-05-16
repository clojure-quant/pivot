(ns demo.doji
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]
   [quanta.calendar.window :as w]
   [quanta.indicator.trailing :refer [add-trailing-high add-trailing-low]]
   [quanta.math.bin :refer [bin]]
   [pivot.calc :refer [calc-multi-window calc-multi-window-multi-assets
                       only-highest-n-per-date
                       load-bars]]
   [pivot.db]
   [pivot.smile :refer [cluster-lines]]
   [pivot.plot :refer [plot-pivots-blue
                       plot-pivots-n-colored
                       plot-pivots-n-colored-lines]]
   [demo.plot-window :refer [plot-bar-window
                             plot-bar-dt-window]]
   [demo.indicator.doji :refer [add-doji select-dojis-at-high-low]]
   [demo.util :refer [print-ds]]
   [demo.env :refer [env]]))

(def calendar [:forex :m])


(def bars
  (load-bars env {:asset "EURUSD"
                  :window (w/create-trailing-window calendar 80000 (t/instant "2025-05-07T20:00:00Z"))}))

bars

(add-doji bars {:min-wick-percent 0.7
                :range-sma-n 60
                :min-range-percent 0.7})


(-> bars
    (select-dojis-at-high-low {:min-wick-percent 0.7
                               :range-sma-n 60
                               :min-range-percent 1.0
                               :trailing-n 50})
    (tc/order-by [:volume]))
; 67 high dojis   
; 70 low dojis

(defn add-ir-bin [ds opts]
  (tc/add-column ds :ir-bin (bin opts (:ir ds)))
  ;(quanta.math.bin/bin-full opts (:ir ds))
  )

(-> bars
    (trailing-highlow-doji-up  {:min-wick-percent 0.7
                                :range-sma-n 60
                                :min-range-percent 0.7
                                :trailing-n 30})
    (add-ir-bin {:n 1000})
    ;(tc/select-rows high+doji?)
    ;(tc/select-rows low+doji?)
    )

(-> bars
    (tc/select-rows (fn [row] (> (:volume row) 500))))


(-> bars
    (tc/select-rows (fn [row]
                      (t/> (:date row) (t/instant "2025-03-20T22:00:00Z")))))

(-> bars
    (tc/select-rows (fn [row] (t/> (:date row) (t/instant "2025-03-20T22:00:00Z"))))
    (tc/select-rows (fn [row] (> (:volume row) 180))))


(-> bars
    (trailing-highlow-doji-up  {:min-wick-percent 0.7
                                :range-sma-n 60
                                :min-range-percent 0.7
                                :trailing-n 30})
    ; up till march 20, we added overnight volume to the opening bar, which was wrong.
    (tc/select-rows (fn [row]
                      (t/> (:date row) (t/instant "2025-03-20T22:00:00Z"))))
    (tc/select-rows (fn [row]  (< (:ir row) 0.004)))
    (add-ir-bin {:n 10})
    (tc/group-by :ir-bin)
    (tc/aggregate {:ir-bin (fn [ds]
                             (-> ds :ir-bin tc/first first))
                   :ir-mean (fn [ds]
                              (-> ds :ir dfn/mean))
                   :count (fn [ds]
                            (->> ds tc/row-count))
                   :volume-mean (fn [ds] (->> ds :volume dfn/mean))
                   :volume-median (fn [ds] (->> ds :volume dfn/median))
                   :volume-min (fn [ds] (->> ds :volume (apply dfn/min)))
                   :volume-max (fn [ds] (->> ds :volume (apply dfn/max)))})
    (tc/order-by [:ir-bin]))