import random

def main ():

    line = input ().split ()
    nRatings = int (line [0])
    nUsers = int (line [1])
    nBooks = int (line [2])

    users = [ {'id': i, 'rated': []} for i in range (nUsers) ]
    books = [ {'id': i} for i in range (nBooks) ]
    ratings = []
    alpha = 0.1

    for i in range (nRatings):
        line = input ().split ()
        user = int (line [0])
        book = int (line [1])
        score = float (line [2]) / 5.0
        ratings.append ((user, book, score))
        users [user]['rated'].append (book)

    nFeatures = 10
    nEpochs = 20

    P = [ [random.random () for i in range (nFeatures)] for j in range (nUsers) ]
    Q = [ [random.random () for i in range (nFeatures)] for j in range (nBooks) ]

    def predict(user, book):
        s = 0.0
        for i in range(nFeatures):
            s += P [user][i] * Q [book][i]
        return s

    for epoch in range (nEpochs):
        totalError = 0.0
        for i in range (nRatings):
            rating = ratings [i]

            u = rating [0]
            b = rating [1]
            a = rating [2]

            p = predict (u, b)
            err = p - a
            err2 = err ** 2.0
            totalError += err2

            for j in range (nFeatures):
                gradP = -2.0 * err * Q[b][j]
                gradQ = -2.0 * err * P[u][j]
                P [u][j] = P [u][j] + alpha * gradP
                Q [b][j] = Q [b][j] + alpha * gradQ

    for u in range (nUsers):
        rec = [ i for i in range (nBooks) ]
        for b in users [u]['rated']:
            rec.remove (b)
        def keyFunction(book):
            return predict (u, book)
        rec = list (reversed (sorted (rec, key = keyFunction))) [0:10]
        for i in range (len(rec)):
            print (f'{rec[i]}', end = '')
            if i != len(rec) - 1:
                print ('\t', end = '')
        print ('')

    pass

if __name__ == '__main__':
    main ()