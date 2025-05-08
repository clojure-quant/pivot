(ns pivot.db
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [juan.util :refer [load-ds-nippy save-ds-nippy spit-ds]]
   [juan.data.pips :refer [->price]]))

(def ds-file ".data/pivots")
(defonce ds-a (atom nil))

(defn get-ds []
  (if-let [ds @ds-a]
    ds
    (let [ds (load-ds-nippy ds-file)]
      (reset! ds-a ds)
      ds)))

(defn save-ds [pivot-ds]
  (spit-ds pivot-ds ds-file)  
  (save-ds-nippy ds-file pivot-ds)
  (reset! ds-a pivot-ds) ; keep cache up to date
  pivot-ds)


(defn load-pivots [asset]
  (let [ds (get-ds)]
    (tc/select-rows ds (fn [row]
                         (= asset (:asset row))))))


(defn get-pivots [{:keys [asset price bar-date max-pip-distance]}]
  (let [price-diff (->price asset max-pip-distance)
        pivot-price-max (+ price price-diff)
        pivot-price-min (- price price-diff)
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
    :max-pip-distance 20})
  ;
  )
