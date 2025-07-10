# ViaLimboService

🌆 A [LimboService](https://github.com/YourCraftMC/LimboService) plugin, 
integrating [ViaProxy](https://github.com/ViaVersion/ViaProxy) for more Minecraft versions to connect.

> [!NOTE]
> This project is forked from [LOOHP's ViaLimbo](https://github.com/LOOHP/ViaLimbo),
> but will be breaking changed and maintained by [YourCraftMC](https://github.com/YourCraftMC).

## Note

Currently, this plugin may output some errors like:
```console
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized format specifier [ip_redactor]
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized conversion specifier [ip_redactor] starting at position 27 in conversion pattern.
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized format specifier [ip_redactor]
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized conversion specifier [ip_redactor] starting at position 67 in conversion pattern.
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized format specifier [ip_redactor]
[XX:XX:XX Error] ... Thread-0 ERROR Unrecognized conversion specifier [ip_redactor] starting at position 64 in conversion pattern.
[XX:XX:XX Error] ... Thread-0 WARN The use of package scanning to locate Log4j plugins is deprecated.
Please remove the deprecated `PluginManager.addPackage()` method call from `net.raphimc.viaproxy.util.logging.Logger.<clinit>(Logger.java:35)`.
See https://logging.apache.org/log4j/2.x/faq.html#package-scanning for details.
```
This is caused by the ViaProxy's Log4j2 configuration, 
which is not compatible with the ViaLimboService's Log4j2 configuration,
but it does not affect the functionality of the plugin, **just ignore it**.

## Acknowledgements & Supports

Many thanks to [LoohpJames(@LOOHP)](https://github.com/LOOHP)
and [many other developers](https://github.com/LOOHP/ViaLimbo/graphs/contributors) for their huge contribution to the
original project.

Many thanks to Jetbrains for kindly providing a license for us to work on this and other open-source projects.

[![](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg)](https://www.jetbrains.com/?from=https://github.com/YourCraftMC/ViaLimboService)

This project currently is mainly maintained by the  [YourCraftMC(你的世界)](https://www.ycraft.cn) .

<img src="https://raw.githubusercontent.com/YourCraftMC/.github/refs/heads/main/imgs/text_1440p.png" alt="Team logo" width="400px">

## Open Source License

This project's source code is licensed under
the [GNU LESSER GENERAL PUBLIC LICENSE](https://www.gnu.org/licenses/lgpl-3.0.html).