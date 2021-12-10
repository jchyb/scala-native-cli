package scala.scalanative.cli

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Paths
import scala.scalanative.cli.utils.ConfigConverter
import scala.scalanative.cli.options.CliOptions
import scala.scalanative.cli.options.LoggerOptions
import scala.scalanative.cli.options.NativeConfigOptions
import scala.scalanative.cli.options.ConfigOptions
import scala.scalanative.build.GC
import scala.scalanative.cli.utils.NativeConfigParserImplicits
import scala.scalanative.build.Mode
import scala.scalanative.build.LTO
import scala.scalanative.cli.options.MiscOptions

class ConfigConverterTest extends AnyFlatSpec {
  val dummyLoggerOptions = LoggerOptions()
  val dummyNativeConfigOptions = NativeConfigOptions()
  val dummyConfigOptions = ConfigOptions(main = Some("Main$"))
  val dummyMiscOptions = MiscOptions()

  val dummyArguments =
    Seq("A.jar", "B.jar")
  val dummyMain = "Main$"

  val dummyCliOptions = CliOptions(
    config = dummyConfigOptions,
    nativeConfig = dummyNativeConfigOptions,
    logger = dummyLoggerOptions,
    misc = dummyMiscOptions
  )

  "ArgParser" should "parse default options" in {
    val config =
      ConfigConverter.convert(dummyCliOptions, dummyMain, dummyArguments)
    assert(config.isRight)
  }

  it should "report incomplete arguments" in {
    val noArgs = Seq()
    val mainOnlyResult =
      ConfigConverter.convert(dummyCliOptions, dummyMain, noArgs)
    assert(mainOnlyResult.isLeft)
    assert(mainOnlyResult.left.get.isInstanceOf[IllegalArgumentException])
  }

  it should "parse classpath strings correctly" in {
    val classPathStrings = Seq(
      "/home/dir/file",
      "/home/dirfile2",
      "/home/dir/with spaces/"
    )
    val expected = Seq(
      Paths.get("/home/dir/file"),
      Paths.get("/home/dirfile2"),
      Paths.get("/home/dir/with spaces/")
    )

    val config =
      ConfigConverter.convert(dummyCliOptions, dummyMain, classPathStrings)

    assert(config != None)
    assert(config.right.get.config.classPath.sameElements(expected))
  }

  it should "handle NativeConfig GC correctly" in {
    def gcAssertion(gcString: String, expectedGC: GC) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(gc =
          NativeConfigParserImplicits.gcParser(None, gcString).right.get
        ),
        dummyLoggerOptions,
        dummyMiscOptions
      )
      val config =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .right
          .get
          .config
      assert(config.compilerConfig.gc == expectedGC)
    }
    gcAssertion("immix", GC.immix)
    gcAssertion("commix", GC.commix)
    gcAssertion("none", GC.none)
    gcAssertion("boehm", GC.boehm)
  }

  it should "handle NativeConfig Mode correctly" in {
    def modeAssertion(modeString: String, expectedMode: Mode) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(mode =
          NativeConfigParserImplicits.modeParser(None, modeString).right.get
        ),
        dummyLoggerOptions,
        dummyMiscOptions
      )
      val config =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .right
          .get
          .config
      assert(config.compilerConfig.mode == expectedMode)
    }
    modeAssertion("debug", Mode.debug)
    modeAssertion("release-fast", Mode.releaseFast)
    modeAssertion("release-full", Mode.releaseFull)
  }

  it should "handle NativeConfig LTO correctly" in {
    def ltoAssertion(ltoString: String, expectedLto: LTO) = {
      val options = CliOptions(
        dummyConfigOptions,
        NativeConfigOptions(lto =
          NativeConfigParserImplicits.ltoParser(None, ltoString).right.get
        ),
        dummyLoggerOptions,
        dummyMiscOptions
      )
      val config =
        ConfigConverter
          .convert(options, dummyMain, dummyArguments)
          .right
          .get
          .config
      assert(config.compilerConfig.lto == expectedLto)
    }
    ltoAssertion("none", LTO.none)
    ltoAssertion("thin", LTO.thin)
    ltoAssertion("full", LTO.full)
  }

  it should "set clang and clang++ correctly" in {
    val clangString = "/tmp/clang"
    val clangPPString = "tmp/clang++"

    val expectedClangPath = Paths.get(clangString)
    val expectedClangPPPath = Paths.get(clangPPString)

    val options = CliOptions(
      dummyConfigOptions,
      NativeConfigOptions(
        clang = Some(clangString),
        clangPP = Some(clangPPString)
      ),
      dummyLoggerOptions,
      dummyMiscOptions
    )

    val nativeConfig =
      ConfigConverter.convert(options, dummyMain, dummyArguments).right.get

    assert(nativeConfig.config.compilerConfig.clang == expectedClangPath)
    assert(nativeConfig.config.compilerConfig.clangPP == expectedClangPPPath)
  }

  it should "parse boolean options as opposite of default" in {
    val options = CliOptions(
      dummyConfigOptions,
      NativeConfigOptions(
        check = true,
        dump = true,
        noOptimize = true,
        linkStubs = true
      ),
      dummyLoggerOptions,
      dummyMiscOptions
    )
    val default = ConfigConverter
      .convert(dummyCliOptions, dummyMain, dummyArguments)
      .right
      .get
      .config
      .compilerConfig
    val nonDefault = ConfigConverter
      .convert(options, dummyMain, dummyArguments)
      .right
      .get
      .config
      .compilerConfig

    assert(nonDefault.check != default.check)
    assert(nonDefault.dump != default.dump)
    assert(nonDefault.optimize != default.optimize)
    assert(nonDefault.linkStubs != default.linkStubs)
  }
}
