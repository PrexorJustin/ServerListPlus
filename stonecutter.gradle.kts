plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Applies version- and loader-specific entries from stonecutter.properties.toml
    properties {
        tags(version, loader)
    }

    // Enables `//? if fabric {` / `//? if neoforge {`
    constants {
        match(loader, "fabric", "neoforge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["modrinth_id"] = "\"${properties.get<String>("publish.modrinth_id")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        // Word boundaries keep `GuiGraphics` from matching inside `GuiGraphicsExtractor`
        regex(current.parsed >= "26.1") {
            replace("\\bGuiGraphics\\b", "GuiGraphicsExtractor", "\\bGuiGraphicsExtractor\\b", "GuiGraphics")
        }
    }
}
