import pusher
import sys
import socket

pusher_client = pusher.Pusher(
  app_id='436344',
  key='36fc8a3649c22f1c1723',
  secret='83d8bcc8210782439810',
  cluster='us2',
  ssl=True
)

filename = sys.argv[1]
ip = socket.gethostbyname(socket.gethostname())

pusher_client.trigger('my-channel', 'my-event', {'message': filename, 'ip': ip})
