(ns demo.plot-window
  (:require
   [tick.core :as t]
   [missionary.core :as m]
     [tablecloth.api :as tc]
   [tech.v3.datatype :as dtype]
   [quanta.calendar.window :as w]
   [ta.db.bars.protocol :as b]
   [cquant.tmlds :refer [clone-ds]]
   [rtable.plot.vega :as plot]
   [demo.env :refer [env]]))

(defn load-bars [env {:keys [asset window]}]
  (-> (m/? (b/get-bars (:bar-db env) {:asset asset} window))
      (clone-ds)))

(defn window-sliding [dt left-n right-n]
  (w/create-event-window [:forex :m] left-n right-n dt))


(defn load-bar-window [env {:keys [asset n-left n-right dt]}]
  (let [window (w/create-event-window [:forex :m] n-left n-right dt)]
    (load-bars env {:asset asset :window window} )))

(defn add-dt-str [ds]
  ; this is needed, because in tooltip vega moves the timezone, so it is unclear which date we really have.
  (let [to-str (fn [dt]
                 (-> dt
                     (t/truncate :minutes)
                     str))]
    (tc/add-column ds :dt-str
                   (dtype/clone (dtype/emap to-str :string (:date ds))))))


(defn plot-close-blue [env ds]
  (plot/vegalite-ds
   env
   {:cols [:close :date :dt-str]
    :spec
    {;:$schema "https://vega.github.io/schema/vega-lite/v4.json"
     :width 800
     :height 800
     :layer [{:mark {:type "point" :color "blue"}
              :encoding {:x {:field "date" :type "temporal"}
                         :y {:field "close" :type "quantitative"
                             :scale {:zero false}}
                         :tooltip [{:field "date" :type "temporal"
                                    :format "%Y-%m-%d %H:%M:%S"}
                                   {:field "dt-str" :type "nominal"}
                                   {:field "close" :type "quantitative"}]
                         
                         }}]}}
   (add-dt-str ds)))

(defn plot-bar-window [env {:keys [asset n-left n-right dt] :as opts}]
  (let [ds (load-bar-window env opts)]
    (plot-close-blue env ds)))

(defn plot-bar-dt-window [env {:keys [asset start end] :as opts}]
  (let [window (w/date-range->window [:forex :m] opts)
        ds (load-bars env {:asset asset :window window})]
    (plot-close-blue env ds)))


(comment 

  (load-bars env {:asset "EURUSD"
                  :window (w/create-trailing-window calendar 80000 (t/instant "2025-05-07T20:00:00Z"))})

  (-> (window-sliding (t/instant) 10 10)
      (w/window->close-range))

  (load-bar-window 
   env
   {:asset "EURUSD"
    :dt (t/instant "2025-05-01T20:00:00Z")
    :n-left 100
    :n-right 100})
  
  (plot-bar-window
   env
   {:asset "EURUSD"
    :dt (t/instant "2025-05-01T20:00:00Z")
    :n-left 100
    :n-right 100})
  ()

  (plot-bar-dt-window
   env
   {:asset "EURUSD"
    :start (t/instant "2025-05-01T20:00:00Z")
    :end (t/instant "2025-05-02T20:00:00Z")
    })
  
  
 ; 
  )

