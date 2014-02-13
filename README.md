fourzei
=======

Fourzei is currently in early beta, and there are lots of areas for improvement. Because Muzei integration requires the use of Muzei's base IntentService classes, working with location and related callbacks on Android poses some challenges. Currently, this means that I need to connect to Google Play services for the current location in a separate Service (FourzeiFourSquareService) to handle the location connection callbacks, and the current clunky implementation means the location can be a bit out of sync at first request. 

*Current TODO:*

* Implement location-based caching to avoid pounding the Foursquare API multiple times upon every update

* Find a better way to synchronize location connection
