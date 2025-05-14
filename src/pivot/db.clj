(ns pivot.db
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [babashka.fs :refer [create-dirs]]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [quanta.bar.db.nippy :as nippy]))

(def dir ".pivots")
(create-dirs dir)

(def ds-file ".pivots/pivots.nippy.gz")
(defonce ds-a (atom nil))

(defn get-ds []
  (if-let [ds @ds-a]
    ds
    (let [ds (nippy/load-ds ds-file)]
      (reset! ds-a ds)
      ds)))


(defn save-ds [pivot-ds]
  (nippy/save-ds ds-file pivot-ds)
  (reset! ds-a pivot-ds) ; keep cache up to date
  pivot-ds)


(defn load-pivots [asset]
  (let [ds (get-ds)]
    (tc/select-rows ds (fn [row]
                         (= asset (:asset row))))))


(defn get-pivots [{:keys [asset price bar-date max-price-distance]}]
  (let [pivot-price-max (+ price max-price-distance)
        pivot-price-min (- price max-price-distance)
        ds (load-pivots asset)
        ;_ (println "get-pivots price [" pivot-price-min " " pivot-price-max "] dt: " bar-date)
        ds-pivots (tc/select-rows ds
                                  (fn [{:keys [pivot-price date date-detected]}]
                                    (and (> pivot-price-max pivot-price pivot-price-min)
                                         (t/< date-detected bar-date))))]
    ds-pivots))


(comment
  (get-ds)
  (load-pivots "EURUSD")
  (load-pivots "USDJPY")

  (get-pivots
   {:asset "USDJPY"
    :price 148.50
    :bar-date (t/instant "2025-04-09T12:39:00Z")
    :max-price-distance 0.20})
  ;
  )
