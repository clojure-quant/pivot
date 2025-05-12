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


(defn calc-pivots [{:keys [ds n asset]}]
  (-> ds
      (p/pivots {:n n})
      (tc/add-column :n n)
      (tc/add-column :asset asset)
      (tc/select-columns [:asset :n :date :pivot-price :pivot-type :pivot-range :pivot-volume :idx :date-detected])
      ))

(defn calc-multi-window
  "calculates pivots for one asset with multiple windows
   window needs to be generated with quanta.calendar
   env needs :bar-db
   "
  [env {:keys [asset window windows] :as opts}]
  ; loads bars only once for all windows that are calculated.
  (let [ds (load-bars env (dissoc opts :windows))
        calc-window (fn [n] (calc-pivots {:ds ds :n n :asset asset}))]
    (->> (map calc-window windows)
         (apply tc/concat))))

(defn calc-multi-window-multi-assets
  "calculates pivots for multiple assets with multiple windows
   window needs to be generated with quanta.calendar
   env needs :bar-db"
  [env {:keys [assets window windows] :as opts}]
  (let [opts (dissoc opts :assets)
        calc-asset (fn [asset]
                     (calc-multi-window env (assoc opts :asset asset)))
        ds-seq (map calc-asset assets)]
    (apply tc/concat ds-seq)))

