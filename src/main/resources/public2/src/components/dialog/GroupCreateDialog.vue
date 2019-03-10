<template>
    <div id="auth-dialog" v-cloak>
        <v-layout row justify-center>
            <v-dialog v-model="dialog" persistent max-width="600px">
                <v-form v-model="valid" ref="form">
                    <v-card>
                        <v-card-title>
                            <span class="strong-title headline">Add a new group</span>
                        </v-card-title>
                        <v-card-text>
                            <v-container grid-list-md>
                                <v-layout wrap>
                                    <v-flex xs12>
                                        <v-text-field outline label="Name*" v-model="name" :rules="nameRules" :counter="36" required autocomplete="name"></v-text-field>
                                    </v-flex>
                                </v-layout>
                            </v-container>
                            <small>*indicates required field</small>
                        </v-card-text>
                        <v-card-actions>
                            <v-spacer></v-spacer>
                            <v-btn color="pink" flat @click="dialog = false">Cancel</v-btn>
                            <v-btn color="pink" flat :disabled="!valid" @click="onCreate">Create</v-btn>
                        </v-card-actions>
                    </v-card>
                </v-form>
            </v-dialog>
        </v-layout>
    </div>
</template>
<script>
import axios from "axios";
import { storageJWT } from "../../var.js";

const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
export default {
    data () {
        return {
            dialog: false,
            valid: false,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.test(v) || 'Username must not contain special characters',
                (v) => v && v.length < 37 || 'Username must be less than 37 characters'
            ]
        }
    },
    methods: {
        setVisible(visible) {
            this.dialog = visible;
            if(visible === true)
                this.$refs.form.reset();
        },
        onCreate() {
            this.$refs.form.validate();
            const url = `${process.env.VUE_APP_BASE_URL}/v2_1/group/add`;
            let that = this;
            axios.put(url, `{ "name": "${this.name}" }`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.dialog = false;
                that.$emit('snackbar', true, `Created group ${that.name}`);
                that.$emit("pushItem");
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to create group: ${e.response.status}`);
            });
        }
    }
};
</script>
