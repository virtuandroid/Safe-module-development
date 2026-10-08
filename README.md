<div align="center">

  # Safe Xposed module development

</div>

### Introduction
This is a repository to explain and demonstrate safe 
Xposed module development. 
Xposed hooks are very powerful, but they introduce 
many opportunities for poor performance, bugs and crashes.


### Safe module development guidelines
The core idea when creating safe hooks is to introduce minimal changes to the hooked app. 
This is achived by hooking as few methods as possible, in only the targeted apps, 
while keeping the hooks performant. Below is a variety of more detailed guidelines useful
when developing Xposed hooks.

---

#### Use narrow hook scopes
Avoid hooking excessive methods to accomplish your task. Hooks create new failure
points and performance degradations. 

**Do's** ✅:

- You should hook as few methods as possible.
- The hooked methods should be used infrequently by the app, if possible
- Use `XposedHelpers.findAndHookMethod()` to hook one specific method

**Don'ts** ❌:

- You should **not** hook broad methods which is used often by other functionality.
For example, you *could* hook `java.lang.StringBuilder.build()`
to intercept all string building in order to change app behavior. 
However, that would create significant performance problems, since the method is invoked
frequently and widely within all apps.

- Do not use `XposedBridge.hookAllMethods()` or `XposedBridge.hookAllConstructors()`
since they will hook excessive methods.

---

#### Check target package or process
Modules can be loaded within any process, including your own 
process. Creating hooks in unexpected apps or processes can lead 
to app crashes or data corruption. You should therefore include checks to 
ensure that you only create hooks in the app you are targeting.

**Do's** ✅:

- Do use lpparam.packageName inside `handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?)`
to check the package name. Use the package name to ensure the module is loaded within the correct process.

**Don'ts** ❌:

- No not install hooks inside an unknown process

---

#### Create idempotent hooks
The hook initialization function *may* be called multiple times during an app's lifecycle.
It is therefore important to create guards to prevent the hooks from 
being registered more than once. 

**Do's** ✅:

- Use Atomic guards such as `AtomicBoolean()`, `Mutex()` or `synchronized()` to ensure hook creation is only run once regardless of threading. An example is shown [here](https://github.com/virtuandroid/Safe-module-development/blob/caf23634fbb0a1c18bed4d02f0a930095e3355b3/example-module/src/main/java/com/example/module/XposedModule.kt#L21-L24).

**Don'ts** ❌:

- Do not use thread-unsafe types such as `Boolean` when writing the hook registration guard  
- Do not use class-specific variables, since `IXposedHookLoadPackage` may be re-created. 
Instead, use a companion object or a singleton object to store the hook state. 

---

#### Create lightweight hooks
Hooks block the normal execution, and runs on the same thread as the hooked function.
Hooks should therefore be lightweight, to prevent blocking the UI, or generally slowing down the
app. 


**Do's** ✅:

- Use Kotlin Coroutines to offload heavy tasks to another thread
- Use a `Channel` to send heavy work to a work queue on a separate thread 
- Cache expensive computation whenever possible, even cached results from 
reflection operations can be significant on often executed hooks

**Don'ts** ❌:

- Do not use Java Threads to schedule heavy work. 
Java Threads are expensive to use repeatedly.

---

#### Account for different Android and app configurations
Method signatures and usage differs between Android versions 
and app versions. Android also enforces different security rules 
depending on which SDK version the app targets. Your module can be 
loaded on any App/Android configuration. 

Therefore, you must use backwards-compatible APIs whenever possible.
One of the best ways of enforcing compatability is setting the project
`minSdk` to 27 (the minimum supported version on modern Xposed frameworks).
Also set `targetSdk` to the latest supported version (currently 37).

**Do's** ✅:
- Set minSdk to 27 (or lower)
- Set targetSdk to the latest version

**Don'ts** ❌:
- Do not create code assumptions reliant on that the hook is attached. 
The method signature can change at any time, which will invalidate the hook.
- Do not rely on new Android APIs (it will prevent backwards compatibility)
- Do not hook Android APIs (they can differ between devices)

---

#### Account for obfuscation
Many Android apps use [r8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
to optimize their compiled code. This effectively randomizes class and function names
across app updates. Consequently, hooks written for one app version may hook another 
method in a newer version. When writing hooks you should therefore avoid
hardcoding hooks for temporary class names (such as `adh.f`)

**Do's** ✅:
- Hook persistent methods which are not optimized by r8
- Find optimized methods by more persistent means such as method signatures and static strings

**Don'ts** ❌:
- Do not use class names to hook r8-optimized classes

---

#### Avoid storing references to large objects
Android contains some large objects such as `Activity` and `Service`. 
These can be convenient to save within hooks, but can lead to unexpected memory leaks.
You should therefore never save direct references to heavy objects whenever possible.

**Do's** ✅:
- Save heavy references using [WeakReference](https://docs.oracle.com/javase/8/docs/api/java/lang/ref/WeakReference.html), 
to allow the garbage collector to collect the object
- Use lightweight or persistent references instead of temporary references. 
For example, use `Context.getApplicationContext()` to get a context reference which 
lives during the entire application runtime, preventing leaks.
- Fetch objects at runtime instead of storing persistent references from startup

**Don'ts** ❌:
- Do not store temporary references
- Do not store heavy references

---

#### Prefer Kotlin to Java during development
Google has announced that future development will be "[Kotlin-first](https://developer.android.com/kotlin/first)".
This means that developer tools will (and is) better supported when using Kotlin.
Additionally, Kotlin provides additionally safety measures, with the most prominent 
measure being Null safety. Therefore, to minimize crashes or other runtime issues module should 
preferably be developed using Kotlin.
