(ns demo.classify
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [pivot.db]
   [pivot.smile :refer [dbscan to-2d-array
                        add-pivot-cluster
                        clustered-pivots
                        remove-non-groups
                        cluster-lines]]
   [demo.env :refer [env assets2]]
   [demo.util :refer [print-ds]]))

(def pivot-ds (pivot.db/load-pivots "EURUSD"))

pivot-ds

(def pivot-prices (to-2d-array (:pivot-price pivot-ds)))

pivot-prices

(-> (dbscan pivot-prices 1 0.0001)
   ;:size
    ;tc/dataset
    ;(pr-str)
    )

252 pivots
57 cluster
-> 5 pivots per cluster on average

(-> (dbscan pivot-prices 1 0.001)
   ;:size
    ;tc/dataset
    ;(pr-str)
    )
; 18 cluster

(-> (dbscan pivot-prices 1 0.01)
   ;:size
    ;tc/dataset
    ;(pr-str)
    )
; 1 cluster

(-> (dbscan pivot-prices 1 0.0005)
   ;:size
    ;tc/dataset
    ;(pr-str)
    )
; 25 cluster

(add-pivot-cluster pivot-ds 1 0.0005)

(-> (clustered-pivots pivot-ds 1 0.0005)
    (remove-non-groups)
    print-ds)

(cluster-lines pivot-ds 1 0.0005)


    
