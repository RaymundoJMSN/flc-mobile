<script lang="ts">
	import ServerGrid from "$components/ServerGrid.svelte";
	import ServerAdd from "$components/ServerAdd.svelte";
</script>

<!-- ponytail: no "Launch Server" tab — it spawns a local NodeJS Foundry, which Android cannot do. -->
<main class="container mx-auto grid max-w-3xl gap-4 px-2 py-3">
	<ServerAdd />
	<ServerGrid />
</main>
