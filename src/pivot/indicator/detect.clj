(ns pivot.indicator.detect
  (:require
   [tech.v3.dataset.rolling :as r]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [tablecloth.api :as tc]))

(defn pivot [p volume tp]
  (when (= p tp)
    [p volume]))

(defn pivot-price [pv]
  (when pv
    (first pv)))

(defn pivot-volume [pvh pvl]
  (cond
    pvh (second pvh)
    pvl (second pvl)
    :else nil))


(defn select-pivots [idx-min idx-max pivot-ds]
  (tc/select-rows pivot-ds
                  (fn [row]
                    (or (and (:pivot-low row)
                             (not (:pivot-high row))
                             (< idx-min (:idx row) idx-max)) ; in begin or end, min/max cannot be detected
                        (and (:pivot-high row)
                             (not (:pivot-low row))
                             (< idx-min (:idx row) idx-max))))))

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
                         :pivot-type pivot-type}))))

(defn pivots
  "input is a tml dataset with bars (open high low close volume)
   and n the number of bars to extend to both sides of each point.
   will return a tml dataset with :pivot-low :pivot-high :pivot-range pivot-volume :idx"
  [bar-ds {:keys [n]}]
  (let [bar-t-ds (r/rolling bar-ds
                            {:window-type :fixed
                             :window-size (inc (* 2 n)) ; 2n+1
                             :relative-window-position :center}
                            {:thigh (r/max :high)
                             :tlow (r/min :low)
                             :date-detected (r/last :date)})
        pivot-low-volume (dtype/emap pivot :object (:low bar-ds) (:volume bar-ds) (:tlow bar-t-ds))
        pivot-high-volume (dtype/emap pivot :object (:high bar-ds) (:volume bar-ds) (:thigh bar-t-ds))

        pivot-low (dtype/clone (dtype/emap pivot-price :float64 pivot-low-volume))
        pivot-high (dtype/clone (dtype/emap pivot-price :float64 pivot-high-volume))

        pivot-volume (dtype/clone (dtype/emap pivot-volume :float64 pivot-low-volume pivot-high-volume))
        ;pivot-low (dtype/clone (dtype/emap pivot :float64 (:low bar-ds) (:volume bar-ds) (:tlow bar-t-ds)))
        ;pivot-high (dtype/clone (dtype/emap pivot :float64 (:high bar-ds) (:volume bar-ds) (:thigh bar-t-ds)))
        row-count (tc/row-count bar-ds)
        bar-pivot-ds (tc/add-columns
                      bar-t-ds
                      {:pivot-low pivot-low
                       :pivot-high pivot-high
                       :pivot-range (dfn/- (:thigh bar-t-ds) (:tlow bar-t-ds))
                       :pivot-volume pivot-volume
                       :idx (range row-count)})]
    (-> (select-pivots n (- row-count n) bar-pivot-ds)
        (consolidate-pivots))))
