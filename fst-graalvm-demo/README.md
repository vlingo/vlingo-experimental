# fst-graalvm-demo
Demo using Fast Serialization And GraalVM...\
This is in conjunction with [github.com/vlingo](https://github.com/vlingo) and [vlingo.io](https://vlingo.io).\
We encountered this issue when trying to run the native image of xoom-designer that use xoom-lattice which uses fast serialization in inter-node messaging in our cluster backed grid compute and data.

## Maven Build
```bash
mvn install
```

## Generate native image resources Configs
```bash
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/fst-graalvm-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Build the native image
```bash
mvn install -Pnative-image
```

## Run the native image
```bash
./target/fst-graalvm
```
## Issue
```bash
./target/fst-graalvm                                                                                    
Exception in thread "main" com.oracle.svm.core.jdk.UnsupportedFeatureError: SerializationConstructorAccessor class not found for declaringClass: [Ljava.lang.Object; (targetConstructorClass: java.lang.Object). Usually adding [Ljava.lang.Object; to serialization-config.json fixes the problem.
	at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:87)
	at com.oracle.svm.reflect.serialize.SerializationSupport.getSerializationConstructorAccessor(SerializationSupport.java:132)
	at jdk.internal.reflect.MethodAccessorGenerator.generateSerializationConstructor(MethodAccessorGenerator.java:48)
	at jdk.internal.reflect.ReflectionFactory.generateConstructor(ReflectionFactory.java:514)
	at jdk.internal.reflect.ReflectionFactory.newConstructorForSerialization(ReflectionFactory.java:427)
	at sun.reflect.ReflectionFactory.newConstructorForSerialization(ReflectionFactory.java:103)
	at org.nustaq.serialization.FSTDefaultClassInstantiator.findConstructorForSerializable(FSTDefaultClassInstantiator.java:110)
	at org.nustaq.serialization.FSTClazzInfo.<init>(FSTClazzInfo.java:137)
	at org.nustaq.serialization.FSTClazzInfoRegistry.getCLInfo(FSTClazzInfoRegistry.java:129)
	at org.nustaq.serialization.FSTClazzNameRegistry.addClassMapping(FSTClazzNameRegistry.java:98)
	at org.nustaq.serialization.FSTClazzNameRegistry.registerClassNoLookup(FSTClazzNameRegistry.java:85)
	at org.nustaq.serialization.FSTClazzNameRegistry.registerClass(FSTClazzNameRegistry.java:81)
	at org.nustaq.serialization.FSTConfiguration.addDefaultClazzes(FSTConfiguration.java:845)
	at org.nustaq.serialization.FSTConfiguration.initDefaultFstConfigurationInternal(FSTConfiguration.java:478)
	at org.nustaq.serialization.FSTConfiguration.createDefaultConfiguration(FSTConfiguration.java:473)
	at org.nustaq.serialization.FSTConfiguration.createDefaultConfiguration(FSTConfiguration.java:465)
	at com.hamzajg.demo.fst.BootstrapApp.main(BootstrapApp.java:7)
```
