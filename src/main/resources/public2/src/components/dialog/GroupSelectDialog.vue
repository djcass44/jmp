<template>
    <div id="dialog-delete" v-cloak>
        <v-layout row justify-center>
            <v-dialog v-model="dialog" max-width="350">
                <v-card>
                    <v-card-title class="headline">Set groups</v-card-title>
                    <div v-if="loading === true" class="text-xs-center pa-4">
                        <v-progress-circular :size="50" color="accent" indeterminate></v-progress-circular>
                    </div>
                    <v-list>
                        <v-subheader>Groups</v-subheader>
                        <v-list-tile v-for="group in groups" :key="group.id" @click="">
                            <v-list-tile-content>
                                <v-list-tile-title>{{ group.name }}</v-list-tile-title>
                            </v-list-tile-content>
                            <v-list-tile-action>
                                <v-checkbox :disabled="loading === true" v-model="group.checked"></v-checkbox>
                            </v-list-tile-action>
                        </v-list-tile>
                    </v-list>
                    <v-card-actions>
                        <v-spacer></v-spacer>
                        <v-btn color="pink" flat="flat" @click="dialog = false">Cancel</v-btn>
                        <v-btn color="pink" flat="flat" @click="apply()">Ok</v-btn>
                    </v-card-actions>
                </v-card>
            </v-dialog>
        </v-layout>
    </div>
</template>
<script>

import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

export default {
    data() {
        return {
            dialog: false,
            loading: true,
            uid: -1,
            groups: [],
            loadGroups: [] // Not modifiable by user
        }
    },
    methods: {
        apply() {
            let that = this;
            for(let i = 0; i < this.groups.length; i++) {
                if(this.groups[i].checked === true && this.loadGroups[i].checked === false) {
                    // Add user to group
                    axios.patch(`${process.env.VUE_APP_BASE_URL}/v2_1/groupmod/add?uid=${this.uid}&gid=${this.groups[i].id}`, {}, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).catch(e => {
                        console.log(e);
                    });
                }
                else if(this.groups[i].checked === false && this.loadGroups[i].checked === true) {
                    // Remove user from group
                    axios.delete(`${process.env.VUE_APP_BASE_URL}/v2_1/groupmod/rm?uid=${this.uid}&gid=${this.groups[i].id}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).catch(e => {
                        console.log(e);
                    });
                }
            }
            this.dialog = false;
        },
        setVisible(visible, uid) {
            this.dialog = visible;
            this.uid = uid;
            if(visible === true)
                this.onCreate();
        },
        setUserGroups(userGroups) {
            for(let i = 0; i < this.groups.length; i++) {
                this.groups[i].checked = userGroups.indexOf(this.groups[i].name) > -1;
            }
            this.loadGroups = JSON.parse(JSON.stringify(this.groups));
        },
        onCreate() {
            this.groups = [];
            this.loadGroups = [];
            // get groups
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/groups`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                response.data.map(item => {
                    if(item.checked === undefined)
                        item.checked = false;
                    that.groups.push(item);
                });

                // get groups user is in
                that.loading = true;
                return axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/user/groups?uid=${that.uid}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                    let userGroups = [];
                    response.data.map(item => {
                        userGroups.push(item.name);
                    });
                    that.setUserGroups(userGroups);
                    that.loading = false;
                }).catch(function(error) {
                    console.log(error);
                    that.loading = false;
                    that.$emit('snackbar', true, `Failed to load groups: ${error.response.status}`);
                });
            }).catch(function(error) {
                console.log(error);
                that.$emit('snackbar', true, `Failed to load groups: ${error.response.status}`);
            });
        }
    }
};
</script>
