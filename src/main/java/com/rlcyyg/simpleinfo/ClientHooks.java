package com.rlcyyg.simpleinfo;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import com.rlcyyg.simpleinfo.client.SimpleInfoConfigScreen;

/**
 * 客户端专属的注册动作，物理隔离在客户端包之外不可见的地方。
 * <p>
 * 主类 {@link SimpleInfo} 通过
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 触碰这里 ——
 * <b>本类只出现在内层 lambda 的字节码里，常量池按需解析</b>，
 * 所以 Dist 不匹配时永远不会被加载，服务端不会碰到 {@link ConfigScreenHandler} 这类客户端类。
 * <p>
 * （注意别把 {@code unsafeRunWhenOn} 换成 {@code safeRunWhenOn}：后者的
 * {@code validateSafeReferent} 会因为「内层内联 lambda 的捕获类 == 实现类」而抛
 * {@code RuntimeException: Unsafe Referent usage found in safe referent method}。）
 * <p>
 * 本类里的 import 全部属于客户端 API，因此绝不能被公共侧类直接 import。
 */
public final class ClientHooks {

    private ClientHooks() {
    }

    /**
     * 注册模组列表"配置"按钮的界面工厂。
     * <p>
     * {@code ConfigScreenHandler} 在 {@code net.minecraftforge.client} 包，
     * <b>不是</b> {@code net.minecraftforge.fml.client} —— 1.20.1 的经典坑。
     * <p>
     * 传 {@link ModLoadingContext} 进来而不是在内部调 {@code ModLoadingContext.get()}：
     * 后者在这个版本已被标记为待删除，而且拿到的是"当前活动容器"，
     * 在模组构造器阶段调用容易拿到错的上下文。
     * <p>
     * {@code ConfigScreenFactory} 有两个构造器重载：
     * {@code Function<Screen,Screen>} 和 {@code BiFunction<Minecraft,Screen,Screen>}。
     * 我们的界面只需要 parent，所以用 Function 那个，别写成 {@code (mc, parent) -> ...}。
     */
    public static void registerConfigScreenFactory(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(SimpleInfoConfigScreen::new));
    }
}
