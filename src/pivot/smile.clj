(ns pivot.smile
  (:require
   [tech.v3.datatype.functional :as dfn]
   [tablecloth.api :as tc]
   [smile.clustering]))

(defn to-2d-array [xs]
  (into-array (map #(double-array [^double %]) xs)))

(defn dbscan [d2_array min-pts radius]
  (let [r (smile.clustering/dbscan d2_array min-pts radius)]
    {:k (.k r)
     :y (.y r)
     :size (.size r)}))

(defn add-pivot-cluster [ds min-pts radius]
  (let [r (dbscan (to-2d-array (:pivot-price ds)) min-pts radius)]
    (tc/add-column ds
                   :cluster (:y r))))

(defn clustered-pivots [ds min-pts radius]
  (-> ds
      (add-pivot-cluster min-pts radius)
      (tc/group-by :cluster)
      (tc/aggregate {:pivot-count (fn [ds] (tc/row-count ds))
                     :pivot-price-median (fn [ds] (dfn/median (:pivot-price ds)))
                     :pivot-price-min (fn [ds] (apply dfn/min (:pivot-price ds)))
                     :pivot-price-max (fn [ds] (apply dfn/max (:pivot-price ds)))})
      (tc/order-by :pivot-price-median)))

(defn remove-non-groups [ds]
  (tc/select-rows
   ds
   (fn [row]
     (not (= (:$group-name row) Integer/MAX_VALUE)))))


(defn cluster-lines [pivot-ds min-pts radius]
  (-> (clustered-pivots pivot-ds min-pts radius)
      (remove-non-groups)
      :pivot-price-median))

(comment

    (def data1 [1.0 1.1
                2.1 2.0
                5.3
                10.0 10.0
                7.4 7.5 7.9 8.1 8.2
                103.4])
    (dbscan (to-2d-array data1) 1 0.5)
  ; {:k 4, :y [0, 0, 1, 1, 2147483647, 2, 2, 3, 3, 3, 3, 3, 2147483647], :size [2, 2, 2, 5, 2]}

    (require '[tablecloth.api :as tc])
    (def ds (tc/dataset {:x data1}))

    ds
    (dbscan (to-2d-array (:x ds)) 1 0.5)
    Integer/MAX_VALUE

  ;
    )