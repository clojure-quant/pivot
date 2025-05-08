(ns juan.indicator.pivot
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

(defn pivots [bar-ds {:keys [n debug?]
                      :or {debug? false}}]
  (let [bar-t-ds (r/rolling bar-ds
                            {:window-type :fixed
                             :window-size (inc (* 2 n)) ; 2n+1
                             :relative-window-position :center}
                            {:thigh (r/max :high)
                             :tlow (r/min :low)
                             :date-detected (r/last :date)
                             })
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
    (if debug?
      bar-pivot-ds
      (select-pivots n (- row-count n) bar-pivot-ds))))
