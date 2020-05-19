# CLJS-D3-Timeline

Interactive d3 timeline, implemented in CLJS.

![Screenshot](https://i.imgur.com/P6EE89w.png)

## Overview

This is a very quick demo of a d3 chart implemented in cljs. Sharing this in hopes it helps answer a few people's google searches. 

- `timeline.data` - Generatively create data with spec and shape it.
- `timeline.timeline` - Creates React component and implements all the data-specific logic.
- `timeline.d3` - Generic D3 functions.
- `timeline.core` - Default entry point.


## D3 Glossary

`selections` - Primary way of manipulating DOM elements in d3. Defines either a single element or group of HTML elements. Selection objects also store extra metadata, event handler data, and chart data. All of the fns below are done unto selections.

`.select/.selectAll` - creates selections' based on id or class. selectAll creates groups (array) of svg elements.

`.data` - d3 way of passing data to selections. Stored in the selection object.

`.enter, .exit` - defines enter/exit interceptors on groups of svg elements (selections). Every fn after a `.enter` is mapped to every element in a selection until we `.exit`.

`.attr, .style` - Add an attribute/style to a HTML element.

`.classed` - takes a classname, and a boolean. The boolean is used to add (true) or remove(false) the classname to/from the selection.

`.append` - adds a child element to the selection. Take a single argument defining the HTML element type.

`.on` - adds an event handler to a selection. It takes 2 args: event type and a handler fn. The handler fn receives 2 args, data and index.

`.call` - calls a function (only once on initialization) with the selection (being acted on) passed as the first argument (+ any additional ones you pass). Idiomatic way of calling d3 functions that generate chart components.

`.node` - gets the first non-null html element of a selection. Useful for grabbing bounding boxes, and x/y offsets for interactive functions.

`this-as this` - CLJS standard fn to get DOM elt being manipulated.

`scales` - 2 sets of scales for each of the x, y chart. Provides a way to linearly map pixel offset values to dates and vice versa (`.invert`)


## Development

Standard figwheel-main project created from leiningen template.

### CIDER

    lein-jack-in-clj&cljs
    CLJS REPL: figwheel-main

### Terminal
To get an interactive development environment run:

    lein fig:build

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

	lein clean

To create a production build run:

	lein clean
	lein fig:min


## License

Copyright © 2018 Ahmed Almutawa

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
