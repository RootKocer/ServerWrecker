const version = "1.20.4"

const mcData = null
const enumReplace = "// VALUES REPLACE"

if (mcData == null) {
  console.error(`Version ${version} not found`)
} else {
  const getNameOfItemId = (id: number): string | null => {
    return mcData.items[id].name.toUpperCase();
  }

  {
    let result = fs.readFileSync("templates/BlockType.java", "utf-8");
    let enumValues: string[] = []
    for (const block of mcData.blocksArray) {
      enumValues.push(`public static final BlockType ${block.name.toUpperCase()} = register(new BlockType(${block.id}, "${block.name}", "${block.displayName}", ${block.hardness ?? -1}F, ${block.stackSize}, ${block.diggable}, ResourceData.BLOCK_PROPERTY_MAP.get("${block.name}"), BlockStateLoader.getBlockShapes("${block.name}")));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/ItemType.java", "utf-8");
    let enumValues: string[] = []
    for (const item of mcData.itemsArray) {
      enumValues.push(`public static final ItemType ${item.name.toUpperCase()} = register(new ItemType(${item.id}, "${item.name}", "${item.displayName}", ${item.stackSize}, ${stringArrayToJavaList(item.enchantCategories)}, ${stringArrayToJavaList(item.repairWith)}, ${item.maxDurability ?? "-1"}));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/EntityType.java", "utf-8");
    let enumValues: string[] = []
    for (const item of mcData.entitiesArray) {
      enumValues.push(`public static final EntityType ${item.name.toUpperCase()} = register(new EntityType(${item.id}, "${item.name}", "${item.displayName}", "${item.type}", ${item.width}F, ${item.height}F, "${item.category}"));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/FoodType.java", "utf-8");
    let enumValues: string[] = []
    for (const food of mcData.foodsArray) {
      enumValues.push(`public static final FoodType ${food.name.toUpperCase()} = register(new FoodType(ItemType.${getNameOfItemId(food.id)}, ${food.foodPoints}, ${food.saturation}, ${food.effectiveQuality}, ${food.saturationRatio}));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }
}

function stringArrayToJavaList(array?: string[]): string {
  if (array == null) {
    return "null"
  }

  return `List.of(${array.map(data => `"${data}"`).join(", ")})`
}
