import pusher
import sys

pusher_client = pusher.Pusher(
  app_id='436344',
  key='36fc8a3649c22f1c1723',
  secret='83d8bcc8210782439810',
  cluster='us2',
  ssl=True
)

filename = sys.argv[1]

pusher_client.trigger('my-channel', 'my-event', {'message': filename})
