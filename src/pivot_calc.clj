(ns study.pivot-calc
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]
   [quanta.bar.transform.shuffle :refer [shuffle-bar-series]]
   [quanta.studio.algo :refer [get-window-trailing]]
   [quanta.studio.safe :refer [get-trailing-bars]]
   [study.env :refer [env]]
   [study.pivot-db :refer [save-ds]]
   [juan.data.asset-pairs :refer [assets2]]
   [juan.indicator.pivot :as p]))

(defn load-bars [{:keys [asset trailing-n calendar dt shuffle?]}]
  (let [bars (m/? (get-trailing-bars env
                                     {:asset asset
                                      :trailing-n trailing-n
                                      :calendar calendar}
                                     dt))]
    (if shuffle?
      (shuffle-bar-series bars)
      bars)))

(defn consolidate-pivots [ds]
  (let [pivot-price-fn (fn [pivot-low pivot-high] (or pivot-low pivot-high))
        pivot-type-fn (fn [pivot-low pivot-high]
                        (cond
                          pivot-low :low
                          pivot-high :high
                          :else :unknown))
        pivot-price (dtype/clone (dtype/emap pivot-price-fn :float64 (:pivot-low ds) (:pivot-high ds)))
        pivot-type (dtype/clone (dtype/emap pivot-type-fn :keyword (:pivot-low ds) (:pivot-high ds)))]
    (-> ds
        (tc/add-columns {:pivot-price pivot-price
                         :pivot-type pivot-type})
        (tc/select-columns [:asset :n :date :pivot-price :pivot-type :pivot-range :pivot-volume :idx :date-detected]))))

(defn calc-pivots [{:keys [ds n asset]}]
  (-> ds
      (p/pivots {:n n :debug? false})
      (tc/select-columns [:date :idx :pivot-low :pivot-high :date-detected :pivot-range :pivot-volume])
      (tc/add-column :n n)
      (tc/add-column :asset asset)
      (consolidate-pivots)))

(defn calc-multi-window [{:keys [asset trailing-n calendar dt shuffle? windows] :as opts}]
  ; loads bars only once for all windows that are calculated.
  (let [ds (load-bars (dissoc opts :windows))
        calc-window (fn [n] (calc-pivots {:ds ds :n n :asset asset}))]
    (->> (map calc-window windows)
         (apply tc/concat))))

(defn calc-multi-window-multi-assets [{:keys [assets trailing-n calendar dt shuffle? windows] :as opts}]
  (let [calc-asset (fn [asset]
                     (calc-multi-window (assoc opts :asset asset)))
        ds-seq (map calc-asset assets)]
    (apply tc/concat ds-seq)))



(calc-multi-window
 {:asset "EURUSD"
  :trailing-n 50000
  :dt (t/instant "2025-04-21T22:00:00Z")
  :calendar [:forex :m]
  :windows [240 480 720 1440
             ;4h 8h  12h 24h
            ]})

(calc-multi-window-multi-assets
 {:assets  assets2
  :trailing-n 50000
  :dt (t/instant "2025-04-21T22:00:00Z")
  :calendar [:forex :m]
  :windows [240 480 720 1440
             ;4h 8h  12h 24h
            ]})

;[3474 8]:
;| :asset |   :n |                :date | :pivot-price | :pivot-type | :pivot-range |  :idx |       :date-detected |
;|--------|-----:|----------------------|-------------:|-------------|-------------:|------:|----------------------|
;| EURUSD |  240 | 2025-03-05T09:33:00Z |     1.072075 |       :high |     0.010175 |   571 | 2025-03-05T13:33:00Z |
;| EURUSD |  240 | 2025-03-06T01:53:00Z |     1.082045 |       :high |     0.003495 |  1484 | 2025-03-06T06:16:00Z |
;| EURUSD |  240 | 2025-03-06T04:06:00Z |     1.078570 |        :low |     0.003660 |  1617 | 2025-03-06T08:29:00Z |
;| EURUSD |  240 | 2025-03-06T07:48:00Z |     1.082230 |       :high |     0.004155 |  1816 | 2025-03-06T11:48:00Z |
;| EURUSD |  240 | 2025-03-06T09:39:00Z |     1.078075 |        :low |     0.006470 |  1927 | 2025-03-06T13:39:00Z |
;| EURUSD |  240 | 2025-03-06T15:04:00Z |     1.085340 |       :high |     0.008770 |  2252 | 2025-03-06T19:04:00Z |
;| EURUSD |  240 | 2025-03-06T18:45:00Z |     1.076570 |        :low |     0.008770 |  2473 | 2025-03-06T23:18:00Z |
;| EURUSD |  240 | 2025-03-07T09:15:00Z |     1.087130 |       :high |     0.006320 |  3310 | 2025-03-07T13:15:00Z |
;| EURUSD |  240 | 2025-03-07T13:36:00Z |     1.088875 |       :high |     0.005330 |  3571 | 2025-03-07T17:36:00Z |
;| EURUSD |  240 | 2025-03-07T18:16:00Z |     1.082560 |        :low |     0.006035 |  3851 | 2025-03-09T21:50:00Z |
;|    ... |  ... |                  ... |          ... |         ... |          ... |   ... |                  ... |
;| USDMXN | 1440 | 2025-03-25T11:23:00Z |    19.956950 |        :low |     0.318950 | 18078 | 2025-03-26T12:04:00Z |
;| USDMXN | 1440 | 2025-04-01T12:33:00Z |    20.541750 |       :high |     0.224800 | 25174 | 2025-04-02T12:47:00Z |
;| USDMXN | 1440 | 2025-04-02T16:00:00Z |    20.514200 |       :high |     0.677600 | 26807 | 2025-04-03T16:05:00Z |
;| USDMXN | 1440 | 2025-04-03T14:28:00Z |    19.836600 |        :low |     0.677600 | 28150 | 2025-04-04T14:41:00Z |
;| USDMXN | 1440 | 2025-04-07T13:48:00Z |    20.806550 |       :high |     0.900900 | 30091 | 2025-04-08T13:59:00Z |
;| USDMXN | 1440 | 2025-04-08T14:06:00Z |    20.470700 |        :low |     0.610600 | 31538 | 2025-04-09T14:43:00Z |
;| USDMXN | 1440 | 2025-04-09T12:48:00Z |    21.081300 |       :high |     0.913450 | 32863 | 2025-04-10T13:01:00Z |
;| USDMXN | 1440 | 2025-04-09T20:44:00Z |    20.167850 |        :low |     0.913450 | 33339 | 2025-04-10T20:57:00Z |
;| USDMXN | 1440 | 2025-04-10T23:55:00Z |    20.627300 |       :high |     0.552300 | 34936 | 2025-04-14T18:13:00Z |
;| USDMXN | 1440 | 2025-04-15T14:05:00Z |    19.928950 |        :low |     0.405550 | 37530 | 2025-04-16T14:26:00Z |
;| USDMXN | 1440 | 2025-04-15T20:08:00Z |    20.147150 |       :high |     0.218200 | 37893 | 2025-04-16T20:29:00Z |


(def pivot-ds
  (calc-multi-window-multi-assets
   {:assets  assets2
    :trailing-n 50000
    :dt (t/instant "2025-04-21T22:00:00Z")
    :calendar [:forex :m]
    :windows [240 480 720 1440
               ;4h 8h  12h 24h
              ]}))

(save-ds pivot-ds)