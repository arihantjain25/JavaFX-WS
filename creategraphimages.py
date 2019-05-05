import pandas as pd
import networkx as nx
import matplotlib.pyplot as plt
import os

path = r'C:\Users\Arihant Jain\IdeaProjects\JavaFX-WS\images'
# path =  '/home/arihant/IdeaProjects/JavaFX-WS/images/'
filelist = [ f for f in os.listdir(path) ]
for f in filelist:
    os.remove(os.path.join(path, f))

f = open('generatedfiles/queries.txt', 'r')
strArr = f.read().split('!!!')
arr = [x.replace('\n', ' ') for x in strArr]
temp_column = arr[1::3]
i = 0
for c in temp_column:
    all_paths = c.split('@')
    from_column, to_column = [], []
    for path in all_paths:
        all_columns = path.split(',')
        if all_columns[0] != '':
            from_column.append(all_columns[0])
            to_column.append(all_columns[1])
    df = pd.DataFrame({'from': from_column, 'to': to_column})
    G = nx.from_pandas_edgelist(df, 'from', 'to', create_using=nx.Graph())
    nx.draw(G, with_labels=True, node_size=1500)
    plt.savefig(path + str(i) + '.png')
    plt.clf()
    i = i + 1
