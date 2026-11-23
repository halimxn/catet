# Room entities are accessed reflectively by generated code only; defaults suffice.
-keep class id.kantong.app.data.** { *; }

# Home-screen widgets: Glance maps widgets by class, so R8 must not merge or rename them.
-keep class id.kantong.app.widget.** { *; }
