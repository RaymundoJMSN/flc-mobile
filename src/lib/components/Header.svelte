<script lang="ts">
	import { getVersion } from "@tauri-apps/api/app";
	import { open } from "@tauri-apps/plugin-shell";

	import { Badge } from "$ui/badge/index.js";
	import * as Tooltip from "$lib/components/ui/tooltip/index.js";

	import ToolbarLightSwitch from "$components/ToolbarLightSwitch.svelte";
	import ToolbarWebGl from "$components/ToolbarWebGL.svelte";
</script>

<header class="bg-background sticky top-0 z-10 w-full border-b">
	<div class="container mx-auto flex w-full max-w-3xl items-center justify-between px-2 py-2">
		<button
			class="flex items-center space-x-1"
			onclick={() => open("https://foundry.ruleplaying.com/flc")}>
			<img
				src="/logo.svg"
				alt="FLC"
				class="size-8" />
			<h1 class="text-xl font-bold">FLC</h1>

			{#await getVersion() then version}
				<Badge
					variant="outline"
					class="font-mono">
					{version}
				</Badge>
			{/await}
		</button>

		<div class="flex items-center space-x-2">
			<Tooltip.Provider>
				<ToolbarLightSwitch />
				<ToolbarWebGl />
			</Tooltip.Provider>
		</div>
	</div>
</header>
