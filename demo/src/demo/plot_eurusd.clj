(ns demo.plot-eurusd
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
   [pivot.smile :refer [cluster-lines]]
   [pivot.plot :refer [plot-pivots-blue
                       plot-pivots-n-colored
                       plot-pivots-n-colored-lines]]
   [demo.plot-window :refer [plot-bar-window
                             plot-bar-dt-window]]
   [demo.util :refer [print-ds]]
   [demo.env :refer [env]]))

(def calendar [:forex :m])

(load-bars env {:asset "EURUSD"
                :window (w/create-trailing-window calendar 80000 (t/instant "2025-05-07T20:00:00Z"))})

(def pivots
  (calc-multi-window
   env
   {:asset "EURUSD"
    :calendar calendar
    :windows [240 480 720 1440
            ;4h 8h  12h 24h
              ]
    :window (w/create-trailing-window calendar 80000 (t/instant "2025-05-07T20:00:00Z"))}))

(-> pivots
    (tc/order-by :date))

(only-highest-n-per-date pivots)


(plot-pivots-blue env pivots)
; send to repl with ctrl+alt+tab with tap wrapped

(plot-pivots-n-colored env pivots)



(-> (plot-pivots-n-colored-lines env pivots [1.10 1.12])
    tap>)

(defn lines [middle step nr]
  (->> (concat
        (map  #(+ middle (* step %)) (range nr))
        (map  #(- middle (* step %)) (range nr)))
       (into [])))

(lines 189.0 1.5 7)


(-> (plot-pivots-n-colored-lines
     env pivots (lines 1.1 0.01 5))
    tap>)


(->
 (plot-pivots-n-colored-lines env
                              pivots
                              (cluster-lines pivots 1 0.0005))
 tap>)

(def n-pivots (only-highest-n-per-date pivots))

(-> (plot-pivots-n-colored-lines
     env n-pivots
     (cluster-lines n-pivots 1 0.0005))
    tap>)

(-> (plot-bar-dt-window
     env
     {:asset "EURUSD"
      :start (t/instant "2025-04-03T22:00:00Z")
      :end (t/instant "2025-04-09T23:00:00Z")})
    tap>)

; 04-06 21:12 low 1.08817
; 04-08 14:36 low 1.0886
; 04-09 18:09 low 1.091315

; undetected highs
; 04-07 12:52 high 1.09869
; 04-08 06:50 high 1.09915

(-> (plot-pivots-n-colored-lines
     env pivots
     (cluster-lines pivots 1 0.001))
    tap>)

; 03-07 13:36 high 1.0887
; 03-11 18:25 high 1.0947

; undetected low
; 03-10 11:06 low 1.0812


(-> (plot-bar-dt-window
     env
     {:asset "EURUSD"
      :start (t/instant "2025-03-07T06:00:00Z")
      :end (t/instant "2025-03-11T22:00:00Z")})
    tap>)



(-> pivots
    (tc/select-rows
     (fn [row]
       (t/= (t/instant "2025-03-26T22:29:00Z") (:date row)))))

(-> pivots
    print-ds)


