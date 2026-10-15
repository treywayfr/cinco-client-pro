package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import gg.cinco.client.ClientModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Keeps the main Combat lists clean: grouped tools live behind the Crystal and Mace pills. */
@Mixin(value = CincoClickGuiScreen.class, remap = false)
public abstract class PvpFolderFilterMixin {
    private static final Set<String> GROUPED = Set.of(
            "Auto Crystal", "Anchor Macro", "Auto Safe Anchor", "Auto Totem", "Offhand Manager",
            "Auto Regear", "Crystal Practice Timer", "Auto Mace", "Breachswap", "Lunge Macro",
            "Auto Jump Reset", "Shield Drain with Mace"
    );

    @Inject(method = "nebulaModules", at = @At("RETURN"), cancellable = true, remap = false)
    private void star$filterNebulaFolders(CallbackInfoReturnable<List<ClientModule>> cir) {
        List<ClientModule> original = cir.getReturnValue();
        boolean combatList = original.stream().anyMatch(m -> m.name.equals("Crystal") || m.name.equals("Mace"));
        if (!combatList) return;
        List<ClientModule> filtered = new ArrayList<>(original);
        filtered.removeIf(m -> GROUPED.contains(m.name));
        cir.setReturnValue(filtered);
    }

    @Inject(method = "orbitModules", at = @At("RETURN"), cancellable = true, remap = false)
    private void star$filterOrbitFolders(String id, CallbackInfoReturnable<List<ClientModule>> cir) {
        if (!"combat".equals(id)) return;
        List<ClientModule> filtered = new ArrayList<>(cir.getReturnValue());
        filtered.removeIf(m -> GROUPED.contains(m.name));
        cir.setReturnValue(filtered);
    }
}
