package kotlinx.benchmark.gradle.mu.internal.utils

//import org.gradle.api.AntBuilder
//import org.gradle.kotlin.dsl.withGroovyBuilder
//
//internal fun AntBuilder.get(
//  @Language("http-url-reference")
//  src: String,
//  dest: File,
//  etag: String,
//) {
//  lifecycleLogLevel = AntBuilder.AntMessagePriority.DEBUG
//
//
//  withGroovyBuilder {
//    "get"(
//      "src" to src,
//      "dest" to dest,
//      "verbose" to true,
//      "quiet" to false,
//    ) {
//      "header"(
//        "name" to "If-None-Match",
//        "value" to etag,
//      )
//    }
//  }
//}
