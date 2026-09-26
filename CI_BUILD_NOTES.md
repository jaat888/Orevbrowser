# CI build notes

The project targets Gradle 8.5, Android Gradle Plugin 8.2.2, Kotlin 1.9.22, compile/target SDK 34 and JDK 17.

Before running `:app:assembleDebug` or `:app:assembleRelease`, provide the real `app/libs/libXray.aar` binary used by the V2Ray/Xray bridge. The source code intentionally keeps this dependency external because a placeholder AAR would produce an APK whose tunnel feature does not actually work.

A no-SDK static validation script is included at `scripts/audit_privbrowse.py`; it checks Feature Center action/key references, MainActivity action branches and activity links.
