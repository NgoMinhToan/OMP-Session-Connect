# ML Kit registers its components through manifest meta-data: on first use,
# MlKitComponentDiscoveryService instantiates every
# com.google.firebase.components.ComponentRegistrar by reflection
# (Class.forName(name).getConstructor().newInstance()).
#
# firebase-components 16.x only ships `-keep class * implements ComponentRegistrar`,
# which preserves the class name but lets R8 shrink the no-arg constructor.
# The registrar then fails to load, ML Kit silently registers no components, and
# BarcodeScanning.getClient() returns a null scanner -> NullPointerException when the
# QR scanner opens in a minified build. firebase-components 19.x fixed this upstream
# with the same rule; keep it here so the app does not depend on the resolved version.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
