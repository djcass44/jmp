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


const jumps = new Vue({
    el: '#main-list',
    data() {
        return {
            items: []
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
const regex = new RegExp('https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)')
const bus = new Vue();
const dialog = new Vue({
    el: '#create-dialog',
    data () {
        return {
            dialog: false,
            valid: false,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => v && v.length < 50 || 'Name must be less than 50 characters'
            ],
            location: '',
            locationRules: [
                (v) => !!v || 'This is a required field.',
                (v) => regex.exec(v) || 'URL must be valid.',
                (v) => v && v.length < 2083 || 'Location must be less than 2083 characters'
            ]
        }
    },
    created () {
        const vm = this;
        bus.$on('dialog', function (value) {
            vm.dialog = value
        })
    },
    methods: {
        submit () {
            this.$refs.form.validate();
            const url = endpoint + 'jumps/add';
            let that = this;
            axios.put(
                url,
                '{ "name": "' + this.name + '", "location": "' + this.location + '" }',
                {headers: {"Content-Type": "application/json"}}
            ).then(r => {
                console.log(r.status);
                that.dialog = false;
                window.location.reload(); // TODO actually update content
            }).catch(e => console.log(e));
        },
        clear () {
            this.$refs.form.reset()
        }
    }
});
new Vue({
    el: "#create-button",
    methods: {
        openDialog: function (event) {
            if(event)
                bus.$emit('dialog', true)
        }
    }
});