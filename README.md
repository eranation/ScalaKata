# Scala Kata

![scala kata instructor mode](Examples/Instructor.png)
*Instructor Mode*

![Html and Markdown Rendering](Examples/From.png)

![Html and Markdown Rendering](Examples/To.png)
*Markdown and Html Rendering*

![Desugaring Scala Code](Examples/Desugaring.png)
*Desugar Scala Code*

## Distributions

### Sbt Plugin

> ### 1. add the plugin to project/plugins.sbt

```scala
addSbtPlugin("com.scalakata" % "plugin" % "0.9.0")
```

> ### 2. and add the settings to your build definition build.sbt

```scala
seq(kataSettings: _*)
```

> ### 3. or in your Build.scala

```scala
com.scalakata.Scalakata.kataSettings
```

> ### 4. start with ```sbt kstart```

### Docker Container

 > ### 1. It's also possible to run scala kata in a docker container:

```
sudo docker run -p 7331:7331 --name scalakata masseguillaume/scalakata:0.9.0
```

 > ### 2. open your browser at http://localhost:7331
