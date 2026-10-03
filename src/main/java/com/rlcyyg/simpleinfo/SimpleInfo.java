package com.rlcyyg.simpleinfo;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 模组入口。纯客户端模组（mods.toml 里已声明 {@code clientSideOnly=true}）。
 * <p>
 * 本类属于公共侧，<b>不得</b> 直接 import 任何 {@code net.minecraft.client.*},
 * 否则一旦被服务端加载就会直接崩。客户端相关注册统一通过
 * {@link ClientHooks#registerConfigScreenFactory} 这条 Dist 隔离的路径走。
 */
@Mod(SimpleInfo.MODID)
public class SimpleInfo {

    public static final String MODID = "simpleinfo";

    public SimpleInfo(FMLJavaModLoadingContext context) {
        // 必须在主类构造器注册，不能挪到 client setup —— config 早于任何 dist 回调加载
        context.registerConfig(ModConfig.Type.CLIENT, SimpleInfoConfig.SPEC, "simpleinfo-client.toml");

        // 模组列表里的"配置"按钮。ConfigScreenHandler 在 net.minecraftforge.client 包，
        // 所以用 Dist 隔离。
        //
        // 用 unsafeRunWhenOn，不要用 safeRunWhenOn —— 后者在开发环境会先跑
        // DistExecutor.validateSafeReferent()，取内层 lambda 的 SerializedLambda，
        // 比较 getCapturingClass() 与 getImplClass()，**两者相等就抛**
        // "Unsafe Referent usage found in safe referent method"。
        // 而内联 lambda 的方法体一定会编译成"所在类"的合成方法（lambda$new$0），
        // 于是捕获类和实现类永远是同一个类 —— 写成内联 lambda 就必然触发这个校验。
        // 想保住 safeRunWhenOn，内层必须写成"指向另一个类的静态方法引用"（implClass != capturingClass），
        // 那样就拿不到 context 了。得不偿失。
        //
        // unsafeRunWhenOn 只跳过这一条 dev 期校验；运行逻辑与 safeRunWhenOn 完全一致
        // （runWhenOn 自己就是直接委托给它的）。这里确实安全：
        // ClientHooks 这个客户端类只出现在内层 lambda 的字节码里，常量池按需解析，
        // Dist 不匹配时 get() 根本不会被调用，服务端永远不会加载它。
        //
        // 两个 () -> 仍需保留：外层是 Supplier<Runnable>，内层才是那个 Runnable。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.registerConfigScreenFactory(context));
    }
}
