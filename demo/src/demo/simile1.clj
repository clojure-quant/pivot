(ns demo.smile1
  (:require
   [pivot.smile :refer [to-2d-array dbscan]]))

(def data1 [1.0 1.1
            2.1 2.0
            5.3
            10.0 10.0
            7.4 7.5 7.9 8.1 8.2
            103.4])


(to-2d-array data1)

(dbscan (to-2d-array data1) 1 0.5)
; {:k 4, :y [0, 0, 1, 1, 2147483647, 2, 2, 3, 3, 3, 3, 3, 2147483647],
;        :size [2, 2, 2, 5, 2]}

(def data [[1.0 2.0]
           [1.1 2.1]
           [10.0 10.0]])

(defn vec->2d-array [v]
  (into-array (map #(double-array %) v)))

(vec->2d-array data)


(dbscan (vec->2d-array data)  1 2.0)
; {:k 1, :y [0, 0, 2147483647], :size [2, 1]}
