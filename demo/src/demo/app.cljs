(ns demo.app
  (:require
   [frontend.css :refer [css-loader]]
   [webly.spa.env :refer [get-resource-path]]
   [dali.flowy.tap]
   ))

(defn wrap-app [page match]
  [:div
   [css-loader (get-resource-path)]
   [page match]])

(defn page [& _]
  [dali.flowy.tap/page])

(def routes
  [["/" {:name 'demo.app/page}]
   ])

 