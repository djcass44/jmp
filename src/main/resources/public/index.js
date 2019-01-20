/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
const endpoint = "http://localhost:7000/v1/";

// Used for triggering actions between Vue instances
const bus = new Vue();

const jumps = new Vue({
    el: '#main-list',
    data() {
        return {
            items: []
        }
    },
    methods: {
        remove(index) {
            let item = this.items[index];
            const url = endpoint + 'jumps/rm/' + item.name;
            let that = this;
            axios.delete(url).then(r => {
                console.log(r.status);
                that.items.splice(index, 1); // Delete the item, making vue update
            }).catch(e => console.log(e));
        },
        edit(index) {
            let item = this.items[index];
            bus.$emit('dialog', true, 'Edit jump point', 'Update', true, item.name, item.location, index);
        },
        highlight(text) {
            return text.replace(new RegExp("https?:\\/\\/(www\\.)?"), match => {
                if(text.startsWith("https"))
                    return `<span class="text-light">${match}</span>`;
                else
                    return `<span class="text-http">${match}</span>`;
            });
        }
    },
    created() {
        const url = endpoint + 'jumps';
        let items = this.items;
        axios.get(url).then(function(response) {
            console.log("Loaded items: " + response.data.length);
            response.data.map(item => {
                items.push(item);
            });
            setTimeout(function() {
                componentHandler.upgradeDom();
                componentHandler.upgradeAllRegistered();
            }, 0);
        }).catch(function(error) {
            console.log(error);
        });
    }
});
const urlRegex = new RegExp('https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)');
const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
const dialog = new Vue({
    el: '#create-dialog',
    data () {
        return {
            dialog: false,
            valid: false,
            edit: false,
            title: 'New jump point',
            action: 'Create',
            lastName: '',
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.exec(v) || 'Name must not contain special characters',
                (v) => v && v.length < 50 || 'Name must be less than 50 characters'
            ],
            location: '',
            locationRules: [
                (v) => !!v || 'This is a required field.',
                (v) => urlRegex.exec(v) || 'URL must be valid.',
                (v) => v && v.length < 2083 || 'Location must be less than 2083 characters'
            ],
            index: -1
        }
    },
    created () {
        const vm = this;
        bus.$on('dialog', function (value, title, action, edit, name, location, index) {
            if(edit)
                vm.edit = edit;
            else {
                vm.edit = false;
                vm.$refs.form.reset();
            }
            vm.dialog = value;
            vm.title = title;
            vm.action = action;
            if(name) {
                vm.name = name;
                vm.lastName = name;
            }
            if(location)
                vm.location = location;
            if(index)
                vm.index = index;
        })
    },
    methods: {
        update () {
            this.$refs.form.validate();
            const url = endpoint + 'jumps/edit';
            let that = this;
            axios.patch(
                url,
                `{ "name": "${this.name}", "location": "${this.location}", "lastName": "${this.lastName}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                // jumps.$data.items = Object.assign({}, jumps.$data.items);
                console.log(that.index);
                console.log(this.index);
                Vue.set(jumps.$data.items, that.index, { name: this.name, location: this.location });
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                bus.$emit('snackbar', true, `Updated ${that.name}`);
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to update ${that.name}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            const url = endpoint + 'jumps/add';
            let that = this;
            axios.put(
                url,
                `{ "name": "${this.name}", "location": "${this.location}" }`,
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                jumps.$data.items.push({
                    name: that.name,
                    location: that.location}
                );
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                bus.$emit('snackbar', true, `Added ${that.name}`)
            }).catch(e => {
                console.log(e);
                bus.$emit('snackbar', true, `Failed to add ${that.name}`);
            });
        },
        clear () {
            this.name = '';
            this.location = '';
            this.$refs.form.reset();
        }
    }
});
new Vue({
    el: "#create-button",
    methods: {
        openDialog: function (event) {
            if(event)
                bus.$emit('dialog', true, 'New jump point', 'Create')
        }
    }
});
new Vue({
    el: "#snackbar",
    data () {
        return {
            snackbar: false,
            timeout: 6000,
            text: 'Hello, I\'m a snackbar'
        }
    },
    created () {
        const vm = this;
        bus.$on('snackbar', function (value, text) {
            vm.snackbar = value;
            vm.text = text;
        })
    },
});
