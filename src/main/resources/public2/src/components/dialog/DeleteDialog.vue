<template>
    <div id="dialog-delete" v-cloak>
        <v-layout row justify-center>
            <v-dialog v-model="dialog" max-width="350">
                <v-card>
                    <v-card-title class="headline">Delete</v-card-title>
                    <v-card-text>Are you sure? This action is immediate and cannot be undone. If you are an admin, be aware that this change may impact functionality for users.</v-card-text>
                    <v-card-actions>
                        <v-spacer></v-spacer>
                        <v-btn color="pink" flat="flat" @click="dialog = false">Cancel</v-btn>
                        <v-btn color="pink" flat="flat" @click="remove()">Delete</v-btn>
                    </v-card-actions>
                </v-card>
            </v-dialog>
        </v-layout>
    </div>
</template>
<script>
export default {
    data() {
        return {
            dialog: false,
            name: '',
            index: -1
        }
    },
    methods: {
        remove() {
            if(this.name === '')
                return;
            if(this.index <= -1)
                return;
            this.$emit('jumpsDoRemove', this.index);
            this.dialog = false;
        },
        setVisible(visible, name, index) {
            this.dialog = visible;
            this.name = name;
            this.index = index;
        }
    }
};
</script>
