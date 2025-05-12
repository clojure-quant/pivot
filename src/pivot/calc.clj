(ns pivot.calc
    (:require
     [missionary.core :as m]
     [tick.core :as t]
     [tablecloth.api :as tc]
     [tech.v3.datatype.functional :as dfn]
     [tech.v3.datatype :as dtype]
     [rtable.plot.vega :as plot]
     [ta.db.bars.protocol :as b]
     [cquant.tmlds :refer [clone-ds]]
     [pivot.indicator.detect :as p]))

(defn load-bars [env {:keys [asset window]}]
  (-> (m/? (b/get-bars (:bar-db env) {:asset asset} window))
      (clone-ds)))

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

(defn calc-multi-window [env {:keys [asset windows] :as opts}]
  ; loads bars only once for all windows that are calculated.
  (let [ds (load-bars env (dissoc opts :windows))
        calc-window (fn [n] (calc-pivots {:ds ds :n n :asset asset}))]
    (->> (map calc-window windows)
         (apply tc/concat))))

(defn calc-multi-window-multi-assets [env {:keys [assets windows] :as opts}]
  (let [opts (dissoc opts :assets)
        calc-asset (fn [asset]
                     (calc-multi-window env (assoc opts :asset asset)))
        ds-seq (map calc-asset assets)]
    (apply tc/concat ds-seq)))

