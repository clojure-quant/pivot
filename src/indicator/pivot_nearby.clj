(ns juan.indicator.pivot-nearby
  (:require
   [tick.core :as t]
   [tech.v3.dataset :as tds]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [tablecloth.api :as tc]
   [study.pivot-db :refer [get-pivots]]
   [juan.data.pips :refer [get-pip-mult]]))

(defn empty->nil [v]
  (if (empty? v) nil v))

(defn get-pivot-data [{:keys [asset price bar-date max-pip-distance] :as opts}]
  (let [pivots-ds (get-pivots opts)
        pivots-above (tc/select-rows pivots-ds (fn [{:keys [pivot-price]}]
                                                 (>= pivot-price price)))
        pivots-below (tc/select-rows pivots-ds (fn [{:keys [pivot-price]}]
                                                 (< pivot-price price)))
        vec-above (->> (:pivot-price pivots-above)
                       (into #{})
                       (into [])
                       (sort <)
                       (into []))
        vec-below (->> (:pivot-price pivots-below)
                       (into #{})
                       (into [])
                       (sort >)
                       (into []))
        pip-m (get-pip-mult asset)
        vec-above-pips (->> vec-above
                            (map (fn [p] (* pip-m (- p price))))
                            (into []))
        vec-below-pips (->> vec-below
                            (map (fn [p] (* pip-m (- price p))))
                            (into []))]
    {:above (empty->nil vec-above)
     :below (empty->nil vec-below)
     :above-pips (empty->nil vec-above-pips)
     :below-pips (empty->nil vec-below-pips)}))



(defn add-pivots
  "adds pivots columns [:pivots-above :pivots-below :pivots-above-pips :pivot-below-pips] to dataset
   :pivots-above is a vector of pivot-prices that are above :close and up to :max-pip-distance from :close
   first item in vector is most nearby pivot. if there are no pivots it returns nil
   options:
     :max-pip-distance 
     :asset asset of the dataset
     :when-row (optional) fn that gets row as map that decides in which row to calculate pivot-vec"
  [bar-ds {:keys [asset max-pip-distance when-row]
           :or {when-row (constantly true)}
           :as opts}]
  (assert max-pip-distance "add-pivots needs :max-pip-distance parameter")
  (assert asset "dd-pivots needs :asset parameter")
  (let [close (:close bar-ds)
        result-maps (map (fn [row]
                           (when (when-row row)
                             (get-pivot-data {:asset asset
                                              :price (:close row)
                                              :bar-date (:date row)
                                              :max-pip-distance max-pip-distance})))
                         (tds/rows bar-ds :mapseq))]
    (tc/add-columns
     bar-ds
     {:pivots-above (map :above result-maps)
      :pivots-below (map :below result-maps)
      :pivots-above-pips (map :above-pips result-maps)
      :pivots-below-pips (map :below-pips result-maps)})))



(comment
  ; pivots as they come out of the db
  (get-pivots
   {:asset "USDJPY"
    :price 148.50
    :bar-date (t/instant "2025-04-09T12:39:00Z")
    :max-pip-distance 20})

  (get-pivot-data
   {:asset "USDJPY"
    :price 148.50
    :bar-date (t/instant "2025-04-09T12:39:00Z")
    :max-pip-distance 20})

  (get-pivot-data
   {:asset "USDJPY"
    :price 1480.50 ; there should be no pivots at 10x price
    :bar-date (t/instant "2025-04-09T12:39:00Z")
    :max-pip-distance 20})


  (def bar-ds (tc/dataset [{:date (t/instant "2025-03-09T12:39:00Z") :close 148.5}
                           {:date (t/instant "2025-04-09T12:39:00Z") :close 148.5}
                           {:date (t/instant "2025-04-10T12:39:00Z") :close 150.5}]))

  bar-ds

  (add-pivots bar-ds
              {:asset "USDJPY"
               :max-pip-distance 20})
  
  (add-pivots bar-ds
             {:asset "USDJPY"
              :max-pip-distance 20
              :when-row (fn [row]
                           (= 150.5 (:close row)))})

;
  )  