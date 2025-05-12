(ns demo.app
  (:require
   [frontend.css :refer [css-loader]]
   [frontend.notification :refer [notification-container]]
   [frontend.dialog :refer [modal-container]]
   [webly.spa.env :refer [get-resource-path]]))

(defn wrap-app [page match]
  [:div
   [modal-container]
   [notification-container]
   [css-loader (get-resource-path)]
   [page match]])

(def routes
  [;["/" {:name 'juan.ui.page/page}]
   ;["/algo-system" {:name 'juan.ui.page/page-algo-system}]
   ;["/juan/:location" {:name 'juan.ui/juan-page}]
   ;["/statistics" {:name 'juan.statistics.ui/statistics-pagege}]
   ])

 