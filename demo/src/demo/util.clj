(ns demo.util
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [tech.v3.dataset.print :as p]
   [quanta.bar.db.nippy :as nippy]
   [transit.core :as transit]))

(defn sanitize-date [ds]
  (-> ds
      (tc/convert-types {:date [:instant #(t/instant %)]})
      ;sanitize-ds
      ))

(defn ensure-date-unique [_env _spec bar-ds]
  (when bar-ds
    (tc/unique-by bar-ds :date)))

(defn ensure-col-float64
  [ds col]
  (let [t (-> ds col meta :datatype)]
    (if (= t :float64)
      ds
      (tc/add-column ds col (map double (col ds))))))

; dataset

(defn print-ds [ds]
  (-> ds
      (p/print-range :all)))

(defn spit-ds [ds-all dsname]
  (->> (p/dataset->str ds-all {:print-index-range :all})
       (spit (str dsname ".txt"))))

(defn save-ds-nippy [filename ds]
  (nippy/save-ds (str filename ".nippy.gz") ds))

(defn load-ds-nippy [filename]
  (nippy/load-ds (str filename ".nippy.gz")))


(defn save-ds-transit [filename ds]
  (transit/spit-transit (str filename ".transit.json") ds))

(defn load-ds-transit [filename]
  (transit/slurp-transit (str filename ".transit.json")))