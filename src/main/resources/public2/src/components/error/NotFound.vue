<template>
    <v-container grid-list-md text-xs-center>
        <v-layout row wrap>
            <v-flex xs12><h1 class="mdl-h1">404</h1></v-flex>
            <v-flex xs12><p class="mdl-h5">The page you're looking for couldn't be found.</p></v-flex>
        </v-layout>
    </v-container>
</template>
<script>
export default {
    name: 'NotFound',
    methods: {
        setLoggedIn() {},
        authChanged() {}
    },
    mounted: function() {
        this.$emit('postInit');
    }
}
</script>
<style scoped>
.mdl-h1 {
    color: #616161;
    font-size: 148px;
    font-weight: 300;
}
.mdl-h5 {
    color: #757575;
    font-size: 32px;
    font-weight: 400;
}
</style>
