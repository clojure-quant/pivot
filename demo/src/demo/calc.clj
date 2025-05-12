(ns demo.calc
  (:require
   [tick.core :as t]
   [quanta.calendar.window :as w]
   [pivot.calc :refer [calc-multi-window calc-multi-window-multi-assets]]
   [pivot.db]
   [demo.env :refer [env assets2]]
   ))

(def calendar [:forex :m])

(calc-multi-window
 env
 {:asset "EURUSD"
  :calendar calendar
  :windows [240 480 720 1440
            ;4h 8h  12h 24h
            ]
  :window (w/create-trailing-window calendar 50000 (t/instant "2025-04-21T22:00:00Z"))})

(calc-multi-window-multi-assets
 env
 {:assets  assets2
  :calendar calendar
  :windows [240 480 720 1440
             ;4h 8h  12h 24h
            ]
  :window (w/create-trailing-window calendar 50000 (t/instant "2025-04-21T22:00:00Z"))
  })

;[3474 8]:
;| :asset |   :n |                :date | :pivot-price | :pivot-type | :pivot-range |  :idx |       :date-detected |
;|--------|-----:|----------------------|-------------:|-------------|-------------:|------:|----------------------|
;| EURUSD |  240 | 2025-03-05T09:33:00Z |     1.072075 |       :high |     0.010175 |   571 | 2025-03-05T13:33:00Z |
;| EURUSD |  240 | 2025-03-06T01:53:00Z |     1.082045 |       :high |     0.003495 |  1484 | 2025-03-06T06:16:00Z |
;| EURUSD |  240 | 2025-03-06T04:06:00Z |     1.078570 |        :low |     0.003660 |  1617 | 2025-03-06T08:29:00Z |
;| EURUSD |  240 | 2025-03-06T07:48:00Z |     1.082230 |       :high |     0.004155 |  1816 | 2025-03-06T11:48:00Z |
;| EURUSD |  240 | 2025-03-06T09:39:00Z |     1.078075 |        :low |     0.006470 |  1927 | 2025-03-06T13:39:00Z |
;| EURUSD |  240 | 2025-03-06T15:04:00Z |     1.085340 |       :high |     0.008770 |  2252 | 2025-03-06T19:04:00Z |
;| EURUSD |  240 | 2025-03-06T18:45:00Z |     1.076570 |        :low |     0.008770 |  2473 | 2025-03-06T23:18:00Z |
;| EURUSD |  240 | 2025-03-07T09:15:00Z |     1.087130 |       :high |     0.006320 |  3310 | 2025-03-07T13:15:00Z |
;| EURUSD |  240 | 2025-03-07T13:36:00Z |     1.088875 |       :high |     0.005330 |  3571 | 2025-03-07T17:36:00Z |
;| EURUSD |  240 | 2025-03-07T18:16:00Z |     1.082560 |        :low |     0.006035 |  3851 | 2025-03-09T21:50:00Z |
;|    ... |  ... |                  ... |          ... |         ... |          ... |   ... |                  ... |
;| USDMXN | 1440 | 2025-03-25T11:23:00Z |    19.956950 |        :low |     0.318950 | 18078 | 2025-03-26T12:04:00Z |
;| USDMXN | 1440 | 2025-04-01T12:33:00Z |    20.541750 |       :high |     0.224800 | 25174 | 2025-04-02T12:47:00Z |
;| USDMXN | 1440 | 2025-04-02T16:00:00Z |    20.514200 |       :high |     0.677600 | 26807 | 2025-04-03T16:05:00Z |
;| USDMXN | 1440 | 2025-04-03T14:28:00Z |    19.836600 |        :low |     0.677600 | 28150 | 2025-04-04T14:41:00Z |
;| USDMXN | 1440 | 2025-04-07T13:48:00Z |    20.806550 |       :high |     0.900900 | 30091 | 2025-04-08T13:59:00Z |
;| USDMXN | 1440 | 2025-04-08T14:06:00Z |    20.470700 |        :low |     0.610600 | 31538 | 2025-04-09T14:43:00Z |
;| USDMXN | 1440 | 2025-04-09T12:48:00Z |    21.081300 |       :high |     0.913450 | 32863 | 2025-04-10T13:01:00Z |
;| USDMXN | 1440 | 2025-04-09T20:44:00Z |    20.167850 |        :low |     0.913450 | 33339 | 2025-04-10T20:57:00Z |
;| USDMXN | 1440 | 2025-04-10T23:55:00Z |    20.627300 |       :high |     0.552300 | 34936 | 2025-04-14T18:13:00Z |
;| USDMXN | 1440 | 2025-04-15T14:05:00Z |    19.928950 |        :low |     0.405550 | 37530 | 2025-04-16T14:26:00Z |
;| USDMXN | 1440 | 2025-04-15T20:08:00Z |    20.147150 |       :high |     0.218200 | 37893 | 2025-04-16T20:29:00Z |


(def pivot-ds
  (calc-multi-window-multi-assets
   env 
   {:assets  assets2
    :calendar calendar
    :windows [240 480 720 1440
               ;4h 8h  12h 24h
              ]
    :window (w/create-trailing-window calendar 50000 (t/instant "2025-04-21T22:00:00Z"))
    }))

pivot-ds

(pivot.db/save-ds pivot-ds)