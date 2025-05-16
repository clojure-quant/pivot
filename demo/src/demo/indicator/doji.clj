(ns demo.indicator.doji
  (:require
   [taoensso.timbre :refer [error]]
   [tablecloth.api :as tc]
   [tech.v3.datatype :as dtype]
   [ta.indicator :as ind]
   [tech.v3.datatype.functional :as dfn]
   [quanta.indicator.trailing :refer [add-trailing-high add-trailing-low]]))


(defn doji-up-wick-one [min-wick-percent o h l c min-r]
  (let [wick-upper  (- h (max o c))
        cir (- h l)]
    ;(println "wick upper:" wick-upper "wick-p: " wick-p)
    (and (> cir min-r)
         (> wick-upper (* min-wick-percent cir)))))

(defn doji-down-wick-one [min-wick-percent o h l c min-r]
  (let [wick-lower  (- (min o c) l)
        cir (- h l)]
    (and (> cir min-r)
         (> wick-lower (* min-wick-percent cir)))))

(defn add-doji
  "doji is a bar with 
   - big upper-wick (- h (max o c)) or big lower-wick (- (min o c) l)
     relative to bar-range (- h l) :min-wick-percent parameter
   - bar-range (-h l) bigger than :min-range-percent of range-sma-n
     (to avoid signals on bars that are smaller than average bars)"
  [bar-ds {:keys [min-wick-percent range-sma-n min-range-percent] :as opts}]
  (assert min-wick-percent " doji needs min-wick-percent parameter ")
  (assert range-sma-n " doji needs range-sma-n parameter ")
  (let [r (ind/ir bar-ds)
        ar (ind/sma {:n range-sma-n} r)
        min-r (dfn/* ar min-range-percent)
        ; dtype/clone is essential. otherwise on large datasets, the mapping will not
      ; be done in sequence, which means that the stateful mapping function will fail.
        wick-down (dtype/clone (dtype/emap (partial doji-down-wick-one min-wick-percent) :boolean
                                             (:open bar-ds)
                                             (:high bar-ds)
                                             (:low bar-ds)
                                             (:close bar-ds)
                                             min-r))
        wick-up (dtype/clone (dtype/emap (partial doji-up-wick-one min-wick-percent) :boolean
                                           (:open bar-ds)
                                           (:high bar-ds)
                                           (:low bar-ds)
                                           (:close bar-ds)
                                           min-r))]
    (tc/add-columns bar-ds
                    {:wick-up wick-up
                     :wick-down wick-down
                     :ir r
                     :ar ar
                     :minr min-r})))


(defn trailing-highlow-doji-up [bars opts]
  (-> bars
      (add-trailing-high (:trailing-n opts))
      (add-trailing-low (:trailing-n opts))
      (add-doji opts)))

(defn high+doji? [{:keys [wick-up trailing-high?]}]
  (and wick-up trailing-high?))

(defn low+doji? [{:keys [wick-down trailing-low?]}]
  (and wick-down trailing-low?))

(defn highlow+doji? [row]
  (or (high+doji? row) (low+doji? row)))


(defn add-doji-type-extreme [bar-ds]
    (let [up? (fn [wick-down trailing-low?] (and wick-down trailing-low?))
          up (dtype/clone (dtype/emap up? :boolean (:wick-down bar-ds) (:trailing-low? bar-ds)))
          down? (fn [wick-up trailing-high?] (and wick-up trailing-high?))
          down (dtype/clone (dtype/emap down? :boolean (:wick-up bar-ds) (:trailing-high? bar-ds)))
          one-high (fn [doji-up doji-down low high close]
                     (cond 
                       doji-down high
                       doji-up close
                       :else nil))
          one-low (fn [doji-up doji-down low high close]
                    (cond
                      doji-down close
                      doji-up low
                      :else nil))
          marker-high (dtype/clone (dtype/emap one-high :double up down (:low bar-ds) (:high bar-ds) (:close bar-ds)))
          marker-low (dtype/clone (dtype/emap one-low :double up down (:low bar-ds) (:high bar-ds) (:close bar-ds)))
          one-type (fn [doji-up doji-down]
                     (cond 
                       doji-up :doji-up
                       doji-down :doji-down
                       :else nil))
          marker-type (dtype/clone (dtype/emap one-type :keyword up down))
          ]
      (tc/add-columns bar-ds 
                      {:doji-marker-high marker-high 
                       :doji-marker-low marker-low
                       :doji-marker-type marker-type
                       })

      ))


(defn select-dojis-at-high-low [bar-ds opts]
  (let [row-count (tc/row-count bar-ds)]
    (-> bar-ds
        (tc/add-column :idx (range row-count))
        (trailing-highlow-doji-up opts)
        (tc/select-rows highlow+doji?)
        (add-doji-type-extreme)
        
        )))

  