import requests
import json

def push_notificate(user_id, text) :
    gcm_url = 'https://android.googleapis.com/gcm/send'

    # CloudMessagingPUSH
    regid = "regID"
    key = "key=" + "API-KEY"

    headers = {'Authorization': key, 'Content-Type': 'application/json'}
    tt = "20160607175422"
    params = json.dumps(\
               {'registration_ids': [regid], \
                'data': {'id': user_id, 'message': text, 'time': tt}})

    r = requests.post(gcm_url, data=params, headers=headers)
    print r.text
    return

if __name__=='__main__' :

    user_id = "sampleID"
    text = "SampleText"
    push_notificate( user_id, text )

